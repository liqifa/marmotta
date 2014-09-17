/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.marmotta.kiwi.sparql.builder;

import org.apache.marmotta.kiwi.persistence.KiWiDialect;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.*;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;

import java.util.LinkedList;

/**
* Collect all statement patterns in a tuple expression.
*
* @author Sebastian Schaffert (sschaffert@apache.org)
*/
public class PatternCollector extends QueryModelVisitorBase<RuntimeException> {

    LinkedList<SQLFragment> parts   = new LinkedList<>();

    int counter = 0;


    private BindingSet bindings;
    private Dataset dataset;
    private ValueConverter converter;
    private KiWiDialect dialect;


    public PatternCollector(TupleExpr expr, BindingSet bindings, Dataset dataset, ValueConverter converter, KiWiDialect dialect) {
        this.bindings = bindings;
        this.dataset = dataset;
        this.converter = converter;
        this.dialect = dialect;

        parts.push(new SQLFragment());
        expr.visit(this);
    }

    @Override
    public void meet(StatementPattern node) throws RuntimeException {
        parts.getLast().getPatterns().add(new SQLPattern("P" + (++counter), node));

        super.meet(node);
    }

    @Override
    public void meet(LeftJoin node) throws RuntimeException {
        node.getLeftArg().visit(this);
        parts.addLast(new SQLFragment());
        if(node.hasCondition()) {
            parts.getLast().getFilters().add(node.getCondition());
        }
        node.getRightArg().visit(this);

    }


    @Override
    public void meet(Filter node) throws RuntimeException {
        parts.getLast().getFilters().add(node.getCondition());

        super.meet(node);
    }

    @Override
    public void meet(Union node) throws RuntimeException {
        // unions are treated as subqueries, don't continue collection, but add the Union to the last part

        parts.getLast().getSubqueries().add(new SQLUnion("U" + (++counter),node, bindings, dataset, converter, dialect));
    }

}
