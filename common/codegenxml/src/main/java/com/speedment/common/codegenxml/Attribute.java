/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.common.codegenxml;

import com.speedment.common.codegenxml.internal.AttributeImpl;
import com.speedment.common.codegenxml.trait.HasIsEscape;
import com.speedment.common.codegenxml.trait.HasName;
import com.speedment.common.codegenxml.trait.HasValue;

/**
 *
 * @author Per Minborg
 */
public interface Attribute extends HasName<Attribute>, HasValue<Attribute>, HasIsEscape<Attribute> {

    static Attribute of(String name) {
        return new AttributeImpl(name);
    }
    
    static Attribute ofUnescaped(String name) {
        return new AttributeImpl(name).setEscape(false);
    }

}