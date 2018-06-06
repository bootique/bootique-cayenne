/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.cayenne.test.persistence3.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

import io.bootique.cayenne.test.persistence3.P3T2;
import io.bootique.cayenne.test.persistence3.P3T3;
import io.bootique.cayenne.test.persistence3.P3T4;

/**
 * Class _P3T1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _P3T1 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "id";

    public static final Property<List<P3T2>> T2S = Property.create("t2s", List.class);
    public static final Property<P3T3> T3 = Property.create("t3", P3T3.class);
    public static final Property<List<P3T4>> T4S = Property.create("t4s", List.class);

    public void addToT2s(P3T2 obj) {
        addToManyTarget("t2s", obj, true);
    }
    public void removeFromT2s(P3T2 obj) {
        removeToManyTarget("t2s", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<P3T2> getT2s() {
        return (List<P3T2>)readProperty("t2s");
    }


    public void setT3(P3T3 t3) {
        setToOneTarget("t3", t3, true);
    }

    public P3T3 getT3() {
        return (P3T3)readProperty("t3");
    }


    public void addToT4s(P3T4 obj) {
        addToManyTarget("t4s", obj, true);
    }
    public void removeFromT4s(P3T4 obj) {
        removeToManyTarget("t4s", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<P3T4> getT4s() {
        return (List<P3T4>)readProperty("t4s");
    }


}
