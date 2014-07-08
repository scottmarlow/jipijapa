/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate4.management;

/**
 * Represents the Hibernate query name which is passed in as a parameter.  The displayQuery can be obtained which
 * has spaces and other symbols replaced with a textual description (which shouldn't be changed or localized.
 * The localization rule is so that one set of admin scripts will work against any back end system.  If it becomes
 * more important to localize the textual descriptions, care should be taken to avoid duplicate values when doing so.
 *
 * @author Scott Marlow
 */
public class QueryName {

    // query name as returned from hibernate Statistics.getQueries()
    private final String hibernateQuery;

    // query name transformed for display use (allowed to be ugly but should be unique)
    private final String displayQuery;

    // HQL symbol or operators
    private static final String SQL_NE = "<>";
    private static final String NE_BANG = "!=";
    private static final String NE_HAT = "^=";
    private static final String LE = "<=";
    private static final String GE = ">=";
    private static final String CONCAT = "||";
    private static final String LT = "<";
    private static final String EQ = "=";
    private static final String GT = ">";
    private static final String OPEN = "(";
    private static final String CLOSE = ")";
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String PLUS = "+";
    private static final String MINUS = "-";
    private static final String STAR = "*";
    private static final String DIV = "/";
    private static final String MOD = "%";
    private static final String COLON = ":";
    private static final String PARAM = "?";
    private static final String COMMA = ",";
    private static final String SPACE = " ";
    private static final String TAB = "\t";
    private static final String NEWLINE = "\n";
    private static final String LINEFEED = "\r";
    private static final String QUOTE = "'";
    private static final String DQUOTE = "\"";
    private static final String TICK = "`";
    private static final String OPEN_BRACE = "{";
    private static final String CLOSE_BRACE = "}";
    private static final String HAT = "^";
    private static final String AMPERSAND = "&";

    // textual representation (not to be localized as we don't won't duplication between any of the values)
    private static final String NOT_EQUAL__ = "_not_equal_";
    private static final String BANG_NOT_EQUAL__ = "_bang_not_equal_";
    private static final String HAT_NOT_EQUAL__ = "_hat_not_equal_";
    private static final String LESS_THAN_EQUAL__ = "_less_than_equal_";
    private static final String GREATER_THAN_EQUAL__ = "_greater_than_equal_";
    private static final String CONCAT__ = "_concat_";
    private static final String LESS_THAN__ = "_less_than_";
    private static final String EQUAL__ = "_equal_";
    private static final String GREATER__ = "_greater_";
    private static final String LEFT_PAREN__ = "_left_paren_";
    private static final String RIGHT_PAREN__ = "_right_paren_";
    private static final String LEFT_BRACKET__ = "_left_bracket_";
    private static final String RIGHT_BRACKET__ = "_right_bracket_";
    private static final String PLUS__ = "_plus_";
    private static final String MINUS__ = "_minus_";
    private static final String STAR__ = "_star_";
    private static final String DIVIDE__ = "_divide_";
    private static final String MODULUS__ = "_modulus_";
    private static final String COLON__ = "_colon_";
    private static final String PARAM__ = "_param_";
    private static final String COMMA__ = "_comma_";
    private static final String SPACE__ = "_space_";
    private static final String TAB__ = "_tab_";
    private static final String NEWLINE__ = "_newline_";
    private static final String LINEFEED__ = "_linefeed_";
    private static final String QUOTE__ = "_quote_";
    private static final String DQUOTE__ = "_double_quote_";
    private static final String TICK__ = "_tick_";
    private static final String OPEN_BRACE__ = "_left_brace_";
    private static final String CLOSE_BRACE__ = "_right_brace_";
    private static final String HAT__ = "_hat_";
    private static final String AMPERSAND__ = "_ampersand_";

    private final StringReplace[] replacements = new StringReplace[] {
        new StringReplace(SQL_NE, NOT_EQUAL__),
            new StringReplace(NE_BANG, BANG_NOT_EQUAL__),
            new StringReplace(NE_HAT, HAT_NOT_EQUAL__),
            new StringReplace(LE, LESS_THAN_EQUAL__),
            new StringReplace(GE, GREATER_THAN_EQUAL__),
            new StringReplace(CONCAT, CONCAT__),
            new StringReplace(LT, LESS_THAN__),
            new StringReplace(EQ, EQUAL__),
            new StringReplace(GT, GREATER__),
            new StringReplace(OPEN, LEFT_PAREN__),
            new StringReplace(CLOSE, RIGHT_PAREN__),
            new StringReplace(OPEN_BRACKET, LEFT_BRACKET__),
            new StringReplace(CLOSE_BRACKET, RIGHT_BRACKET__),
            new StringReplace(PLUS, PLUS__),
            new StringReplace(MINUS, MINUS__),
            new StringReplace(STAR, STAR__),
            new StringReplace(DIV, DIVIDE__),
            new StringReplace(MOD, MODULUS__),
            new StringReplace(COLON, COLON__),
            new StringReplace(PARAM, PARAM__),
            new StringReplace(COMMA, COMMA__),
            new StringReplace(SPACE, SPACE__),
            new StringReplace(TAB, TAB__),
            new StringReplace(NEWLINE, NEWLINE__),
            new StringReplace(LINEFEED, LINEFEED__),
            new StringReplace(QUOTE, QUOTE__),
            new StringReplace(DQUOTE, DQUOTE__),
            new StringReplace(TICK, TICK__),
            new StringReplace(OPEN_BRACE, OPEN_BRACE__),
            new StringReplace(CLOSE_BRACE, CLOSE_BRACE__),
            new StringReplace(HAT, HAT__),
            new StringReplace(AMPERSAND, AMPERSAND__)
    };

    public static QueryName queryName(String query) {
        return new QueryName(query);
    }

    /**
     * Construct
     *
     * @param query
     */
    public QueryName(String query) {
        hibernateQuery = query;
        displayQuery = displayable(query);

    }

    public String getDisplayName() {
        return displayQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryName queryName = (QueryName) o;

        if (displayQuery != null ? !displayQuery.equals(queryName.displayQuery) : queryName.displayQuery != null)
            return false;
        if (hibernateQuery != null ? !hibernateQuery.equals(queryName.hibernateQuery) : queryName.hibernateQuery != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hibernateQuery != null ? hibernateQuery.hashCode() : 0;
        result = 31 * result + (displayQuery != null ? displayQuery.hashCode() : 0);
        return result;
    }

    /**
     * transform a Hibernate HQL query into something that can be displayed/used for management operations
     *
     * @param query
     * @return
     */
    private String displayable(String query) {
        if (query == null ||
                query.length() == 0) {
            return query;
        }

        return StringReplace.replace(query, replacements);
    }

    public static void main(String[] args) {

        String test = "select * from employee e (where e.id <> 0 and e.name ='foo' and e.other != 'other'";
        long start = System.currentTimeMillis();

        for ( int looper = 0; looper < 500000; looper ++) {
            QueryName name = new QueryName(test);
        }
        long end = System.currentTimeMillis();
        System.out.println("took " + (end- start) + "ms, input= " + test + ", displayable =" + new QueryName(test).getDisplayName());

    }


}
