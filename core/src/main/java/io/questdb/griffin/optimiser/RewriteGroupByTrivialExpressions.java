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

import io.questdb.griffin.PostOrderTreeTraversalAlgo;
import io.questdb.griffin.SqlException;
import io.questdb.griffin.model.ExpressionNode;
import io.questdb.griffin.model.QueryModel;
import io.questdb.std.CharSequenceIntHashMap;
import io.questdb.std.Chars;
import io.questdb.std.ObjList;

import static io.questdb.griffin.model.ExpressionNode.*;

public class RewriteGroupByTrivialExpressions {

    public static final RewriteGroupByTrivialExpressionsVisitor INSTANCE = new RewriteGroupByTrivialExpressionsVisitor();
    public static final CountLiteralAppearancesVisitor countLiteralAppearancesVisitor = new CountLiteralAppearancesVisitor();
    public static final OneLiteralAndConstantsVisitor oneLiteralAndConstantsVisitor = new OneLiteralAndConstantsVisitor();

    public static QueryModel run(QueryModel model, PostOrderTreeTraversalAlgo postOrderTreeTraversalAlgo, QueryModelTraverser queryModelTraverser) throws SqlException {
        queryModelTraverser.traverse(model, INSTANCE.of(postOrderTreeTraversalAlgo));
        return model;
    }

    /**
     * Examines group by nodes to look for literals.
     */
    private static boolean isOnlyLiteralExpressions(ObjList<ExpressionNode> exprs) {
        for (int i = 0, n = exprs.size(); i < n; i++) {
            final ExpressionNode node = exprs.getQuick(i);
            if (node.type != LITERAL) {
                return false;
            }
        }
        return true;
    }

    public static class CountLiteralAppearancesVisitor implements PostOrderTreeTraversalAlgo.Visitor {
        private CharSequenceIntHashMap counts;

        public CountLiteralAppearancesVisitor of(CharSequenceIntHashMap counts) {
            this.counts = counts;
            return this;
        }

        @Override
        public void visit(ExpressionNode node) {
            switch (node.type) {
                case LITERAL:
                    this.counts.putIfAbsent(node.token, 0);
                    this.counts.increment(node.token);
            }
        }
    }

    public static class OneLiteralAndConstantsVisitor implements PostOrderTreeTraversalAlgo.Visitor {

        private CharSequence allowedLiteral = null;

        @Override
        public void visit(ExpressionNode node) {
            switch (node.type) {
                case LITERAL:
                    if (allowedLiteral == null) {
                        allowedLiteral = node.token;
                    }
                    if (!Chars.equalsIgnoreCase(node.token, allowedLiteral)) {
                        throw RewriteGroupByTrivialExpressionsException.INSTANCE;
                    }
                    return;
                case CONSTANT:
                case OPERATION:
                    return;
                default:
                    throw RewriteGroupByTrivialExpressionsException.INSTANCE;
            }
        }
    }

    private static class RewriteGroupByTrivialExpressionsException extends RuntimeException {
        private static final RewriteGroupByTrivialExpressionsException INSTANCE = new RewriteGroupByTrivialExpressionsException();
    }

    public static class RewriteGroupByTrivialExpressionsVisitor implements QueryModelTraverser.Visitor {
        PostOrderTreeTraversalAlgo expressionTraverser;

        public RewriteGroupByTrivialExpressionsVisitor of(PostOrderTreeTraversalAlgo postOrderTreeTraversalAlgo) {
            expressionTraverser = postOrderTreeTraversalAlgo;
            return this;
        }

        @Override
        public QueryModel visit(QueryModel curr, QueryModel next) throws SqlException {
            // nullcheck
            if (curr == null) {
                return curr;
            }

            // ensure the pattern matches
            if (!(curr.isSelectChoose() && next != null && next.isSelectNone() && next.hasGroupBy())) {
                return;
            }

            // If its of the form GROUP BY A, B, C
            // Then we don't need to do anything at this stage.
            // Constants TBA
            if (isOnlyLiteralExpressions(next.getGroupBy())) {
                return;
            }

            // let us ignore the function for now, its handled later I think
            for (int i = 0, n = next.getGroupBy().size(); i < n; i++) {
                try {
                    expressionTraverser.traverse(next.getGroupBy().getQuick(i), oneLiteralAndConstantsVisitor);
                } catch (Exception e) {
                    return;
                }
            }

            CharSequenceIntHashMap literalAppearanceCount = new CharSequenceIntHashMap();

            // let us ignore the function for now, its handled later I think
            for (int i = 0, n = next.getGroupBy().size(); i < n; i++) {
                expressionTraverser.traverse(next.getGroupBy().getQuick(i), countLiteralAppearancesVisitor.of(literalAppearanceCount));
            }


            // if any counts are >1
            // look for the trivial expressions
            // lift them to the select
            // if they don't appear in the outer select, there's no point grouping them
            // i.e SELECT A, A + 1
            // GROUP BY A, A+1, A+2
            // A+ 2 is pointless

            return;
        }
    }
}
