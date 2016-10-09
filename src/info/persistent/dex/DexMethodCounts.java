/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.persistent.dex;

import com.android.dexdeps.ClassRef;
import com.android.dexdeps.DexData;
import com.android.dexdeps.FieldRef;
import com.android.dexdeps.MethodRef;
import com.android.dexdeps.Output;
import com.github.spyhunter99.model.CountData;
import com.github.spyhunter99.model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DexMethodCounts extends DexCount {

    DexMethodCounts() {
        super();
    }

    @Override
    public CountData generate(DexData dexData, boolean includeClasses, String packageFilter, int maxDepth, Filter filter) {
        MethodRef[] methodRefs = getMethodRefs(dexData, filter);
        CountData ret = new CountData();
        FieldRef[] fieldRefs = getFieldRefs(dexData, filter);


        for (MethodRef methodRef : methodRefs) {
            String classDescriptor = methodRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            ret.overallMetrics.methodCount++;


            String packageNamePieces[] = packageName.split("\\.");
            Node packageNode = ret.packageTree;
            if (packageNode == null) {
                packageNode = ret.packageTree = new Node();
            }

            for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                packageNode.count.methodCount++;
                String name = packageNamePieces[i];
                if (packageNode.children.containsKey(name)) {
                    packageNode = packageNode.children.get(name);
                } else {
                    Node childPackageNode = new Node();
                    if (name.length() == 0) {
                        // This method is declared in a class that is part of the default package.
                        // Typical examples are methods that operate on arrays of primitive data types.
                        name = "(default)";
                    }
                    packageNode.children.put(name, childPackageNode);
                    packageNode = childPackageNode;
                }
            }
            packageNode.count.methodCount++;

        }

        for (FieldRef fieldRef : fieldRefs) {
            String classDescriptor = fieldRef.getDeclClassName();
            String packageName = includeClasses ?
                    Output.descriptorToDot(classDescriptor).replace('$', '.') :
                    Output.packageNameOnly(classDescriptor);
            if (packageFilter != null &&
                    !packageName.startsWith(packageFilter)) {
                continue;
            }
            ret.overallMetrics.fieldCount++;

             String packageNamePieces[] = packageName.split("\\.");
                Node packageNode = ret.packageTree;
                if (packageNode == null) {
                    packageNode = ret.packageTree = new Node();
                }
                for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                    packageNode.count.fieldCount++;
                    String name = packageNamePieces[i];
                    if (name.length() == 0) {
                        // This field is declared in a class that is part of the default package.
                        name = "(default)";
                    }
                    if (packageNode.children.containsKey(name)) {
                        packageNode = packageNode.children.get(name);
                    } else {
                        Node childPackageNode = new Node();

                        packageNode.children.put(name, childPackageNode);
                        packageNode = childPackageNode;
                    }
                }
                packageNode.count.fieldCount++;
               // ret.packageTree = packageNode;

        }
        return ret;
    }

    private static FieldRef[] getFieldRefs(DexData dexData, Filter filter) {
        FieldRef[] fieldRefs = dexData.getFieldRefs();
        // out.println("Read in " + fieldRefs.length + " field IDs from " + dexData.getDexFileName() + ".");
        if (filter == Filter.ALL) {
            return fieldRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        //out.println("Read in " + externalClassRefs.length + " external class references.");
        Set<FieldRef> externalFieldRefs = new HashSet<FieldRef>();
        for (ClassRef classRef : externalClassRefs) {
            Collections.addAll(externalFieldRefs, classRef.getFieldArray());
        }
        //out.println("Read in " + externalFieldRefs.size() + " external field references.");
        List<FieldRef> filteredFieldRefs = new ArrayList<FieldRef>();
        for (FieldRef FieldRef : fieldRefs) {
            boolean isExternal = externalFieldRefs.contains(FieldRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal)
                    || (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredFieldRefs.add(FieldRef);
            }
        }
        //out.println("Filtered to " + filteredFieldRefs.size() + " " +
        //      (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") + " field IDs.");
        return filteredFieldRefs.toArray(new FieldRef[filteredFieldRefs.size()]);
    }


    private static MethodRef[] getMethodRefs(DexData dexData, Filter filter) {
        MethodRef[] methodRefs = dexData.getMethodRefs();
        // out.println("Read in " + methodRefs.length + " method IDs from " + dexData.getDexFileName() + ".");
        if (filter == Filter.ALL) {
            return methodRefs;
        }

        ClassRef[] externalClassRefs = dexData.getExternalReferences();
        //out.println("Read in " + externalClassRefs.length +
        //      " external class references.");
        Set<MethodRef> externalMethodRefs = new HashSet<MethodRef>();
        for (ClassRef classRef : externalClassRefs) {
            Collections.addAll(externalMethodRefs, classRef.getMethodArray());
        }
        //out.println("Read in " + externalMethodRefs.size() +
        //      " external method references.");
        List<MethodRef> filteredMethodRefs = new ArrayList<MethodRef>();
        for (MethodRef methodRef : methodRefs) {
            boolean isExternal = externalMethodRefs.contains(methodRef);
            if ((filter == Filter.DEFINED_ONLY && !isExternal) ||
                    (filter == Filter.REFERENCED_ONLY && isExternal)) {
                filteredMethodRefs.add(methodRef);
            }
        }
        //out.println("Filtered to " + filteredMethodRefs.size() + " " +
        //      (filter == Filter.DEFINED_ONLY ? "defined" : "referenced") +
        //    " method IDs.");
        return filteredMethodRefs.toArray(
                new MethodRef[filteredMethodRefs.size()]);
    }
}
