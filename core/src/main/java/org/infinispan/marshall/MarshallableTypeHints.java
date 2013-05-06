/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

package org.infinispan.marshall;

import java.util.concurrent.ConcurrentMap;

import org.infinispan.util.concurrent.ConcurrentMapFactory;
import org.infinispan.util.logging.ALogger;
import org.infinispan.util.logging.LogFactory;

/**
 * Class providing hints about marshallable types, such as whether a particular
 * type is marshallable or not, or an accurate approach to the serialized
 * size of a particular type.
 *
 * @author Galder Zamarreño
 * @since 5.1
 */
public final class MarshallableTypeHints {

   private static final ALogger log = LogFactory.getLog(MarshallableTypeHints.class);
   private static final boolean trace = log.isTraceEnabled();

   /**
    * Cache of classes that are considered to be marshallable alongside their
    * buffer size predictor. Since checking whether a type is marshallable
    * requires attempting to marshalling them, a cache for the types that are
    * known to be marshallable or not is advantageous.
    */
   private final ConcurrentMap<Class<?>, MarshallingType> typeHints =
         ConcurrentMapFactory.makeConcurrentMap();

   /**
    * Get the serialized form size predictor for a particular type.
    *
    * @param type Marshallable type for which serialized form size will be predicted
    * @return an instance of {@link BufferSizePredictor}
    */
   public BufferSizePredictor getBufferSizePredictor(Class<?> type) {
      MarshallingType marshallingType = typeHints.get(type);
      if (marshallingType == null) {
         // Initialise with isMarshallable to null, meaning it's unknown
         marshallingType = new MarshallingType(null, new AdaptiveBufferSizePredictor());
         MarshallingType prev = typeHints.putIfAbsent(type, marshallingType);
         if (prev != null) {
            marshallingType = prev;
         } else {
            if (trace) {
               log.trace("Cache a buffer size predictor for '" + type.getName() + "' assuming " +
                     "its serializability is unknown");
            }
         }
      }
      return marshallingType.sizePredictor;
   }

   /**
    * Returns whether the hint on whether a particular type is marshallable or
    * not is available. This method can be used to avoid attempting to marshall
    * a type, if the hints for the type have already been calculated.
    *
    * @param type Marshallable type to check whether an attempt to mark it as
    *             marshallable has been made.
    * @return true if the type has been marked as marshallable at all, false
    * if no attempt has been made to mark the type as marshallable.
    */
   public boolean isKnownMarshallable(Class<?> type) {
      MarshallingType marshallingType = typeHints.get(type);
      return marshallingType != null && marshallingType.isMarshallable != null;
   }

   /**
    * Returns whether a type can be serialized. In order for a type to be
    * considered marshallable, the type must have been marked as marshallable
    * using the {@link #markMarshallable(Class, boolean)} method earlier,
    * passing true as parameter. If a type has not yet been marked as
    * marshallable, this method will return false.
    */
   public boolean isMarshallable(Class<?> type) {
      MarshallingType marshallingType = typeHints.get(type);
      if (marshallingType != null) {
         Boolean marshallable = marshallingType.isMarshallable;
         return marshallable != null ? marshallable.booleanValue() : false;
      }

      return false;
   }

   /**
    * Marks a particular type as being marshallable or not being not marshallable.
    *
    * @param type Class to mark as serializable or non-serializable
    * @param isMarshallable Whether the type can be marshalled or not.
    */
   public void markMarshallable(Class<?> type, boolean isMarshallable) {
      MarshallingType marshallType = typeHints.get(type);
      if (marshallableUpdateRequired(isMarshallable, marshallType)) {
         boolean replaced = typeHints.replace(type, marshallType, new MarshallingType(
               Boolean.valueOf(isMarshallable), marshallType.sizePredictor));
         if (replaced && trace) {
            log.trace("Replacing '" + type.getName() + "' type to be marshallable=" + isMarshallable);
         }
      } else if (marshallType == null) {
         if (trace) {
        	 log.trace("Replacing '" + type.getName() + "' type to be marshallable=" + isMarshallable);
         }

         typeHints.put(type, new MarshallingType(
               Boolean.valueOf(isMarshallable), new AdaptiveBufferSizePredictor()));
      }
   }

   /**
    * Clear the cached marshallable type hints.
    */
   public void clear() {
      typeHints.clear();
   }

   private boolean marshallableUpdateRequired(boolean isMarshallable,
         MarshallingType marshallType) {
      return marshallType != null &&
            (marshallType.isMarshallable == null ||
                   marshallType.isMarshallable.booleanValue() != isMarshallable);
   }

   private static class MarshallingType {

      final Boolean isMarshallable;
      final BufferSizePredictor sizePredictor;

      private MarshallingType(Boolean marshallable, BufferSizePredictor sizePredictor) {
         isMarshallable = marshallable;
         this.sizePredictor = sizePredictor;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         MarshallingType that = (MarshallingType) o;

         if (isMarshallable != null ? !isMarshallable.equals(that.isMarshallable) : that.isMarshallable != null)
            return false;
         if (sizePredictor != null ? !sizePredictor.equals(that.sizePredictor) : that.sizePredictor != null)
            return false;

         return true;
      }

      @Override
      public int hashCode() {
         int result = isMarshallable != null ? isMarshallable.hashCode() : 0;
         result = 31 * result + (sizePredictor != null ? sizePredictor.hashCode() : 0);
         return result;
      }
   }

}
