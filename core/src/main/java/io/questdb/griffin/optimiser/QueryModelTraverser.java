/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.optimiser;

import io.questdb.griffin.SqlException;
import io.questdb.griffin.model.QueryModel;

public final class QueryModelTraverser {
    QueryModel curr;
    QueryModel next;
    QueryModel prev;
    QueryModel top;

    public void traverse(QueryModel model, Visitor visitor) throws SqlException {
        prev = null;
        curr = model;
        next = model.getNestedModel();
        while (curr != null) {
            curr = visitor.visit(curr, next);
            if (prev == null) {
                prev = curr;
                curr = next;
                next = curr.getNestedModel();
            } else {
                prev.setNestedModel(curr);
                curr = next;
                next = curr.getNestedModel();
            }
        }
    }

    // visitor returns the 'new current model'
    public interface Visitor {
        QueryModel visit(QueryModel curr, QueryModel next) throws SqlException;
    }

}