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

import com.android.dexdeps.DexData;
import com.github.spyhunter99.model.CountData;

import java.io.PrintStream;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public abstract class DexCount {


    DexCount() {
    }

    public abstract CountData generate(
            DexData dexData, boolean includeClasses, String packageFilter, int maxDepth, Filter filter);


    enum Filter {
        ALL,
        DEFINED_ONLY,
        REFERENCED_ONLY
    }



}
