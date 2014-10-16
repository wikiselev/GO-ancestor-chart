package uk.ac.ebi.quickgo.web.servlets.annotation;

import uk.ac.ebi.interpro.common.collections.*;

import java.util.*;
import uk.ac.ebi.quickgo.web.data.RelationType;

public class AdvancedRequest {
    private final String defaultRelTypes;
    interface Like {
        boolean test(StringProgress sp);
    }

    static class Any implements Like {
        BitSet ok=new BitSet();
        String ch;
        Any(String chars,int... characterType) {
            ch=chars;
            for (int i : characterType) {
                ok.set(i);
            }
        }
        public boolean test(StringProgress sp) {
            if (sp.index>=sp.text.length()) return false;
            char c = sp.text.charAt(sp.index);
            if (ch.indexOf(c)<0 && !ok.get(Character.getType(c))) return false;
            sp.index++;
            return true;
        }
    }



    static class Is implements Like {
        String test;
        public boolean test(StringProgress sp) {
            int end = sp.index + test.length();
            if (end >sp.text.length()) return false;
            if (!sp.text.substring(sp.index, end).equals(test)) return false;
            sp.index=end;
            return true;
        }

        public Is(String test) {
            this.test = test;
        }
    }

    static class Or implements Like {
        Like[] options;

        public Or(Like[] options) {
            this.options = options;
        }

        public boolean test(StringProgress sp) {
            for (Like option : options) {
                if (option.test(sp)) return true;
            }
            return false;
        }
    }

    static class Multi implements Like {
        Like item;
        public boolean test(StringProgress sp) {
            int ct=0;
            while (true) {
                if (!item.test(sp)) break;
                ct++;
            }
            return ct>0;
        }

        public Multi(Like item) {
            this.item = item;
        }
    }

    public static class ParseException extends Exception {
        public StringProgress where;
        public String error;
        public String prefix() {return where.text.substring(0,where.index);}
        public String postfix() {return where.text.substring(where.index);}

        public ParseException(StringProgress where, String error) {
            this.where = where;
            this.error = error;
        }
    }

    static class StringProgress {

        public StringProgress(String text) {
            this.text = text;
        }

        int index;
        String text;
        String find(Like like) {
            int start=index;
            if (!like.test(this)) return null;
            return text.substring(start,index);
        }
        void ignore(Like like) {
            like.test(this);
        }
        boolean test(Like like) {
            return like.test(this);
        }
        void expect(Like like,String message) throws ParseException {
            if (!like.test(this)) throw new ParseException(this,message);
        }
        boolean eof() {return index==text.length();}
        static Like is(String test) {
            return new Is(test);
        }
        static Like any(String chars,int... characterType) {
            return new Any(chars,characterType);
        }
        static Like many(String chars,int... characterType) {
            return new Multi(new Any(chars,characterType));
        }
    }


    public String text;
    public Filter root;
    public ParseException broke;
    boolean active;
    private static final Like WHITE_SPACE = StringProgress.many(" \r\n\t");

    public String toString() {
        return root.toString();
    }

    AdvancedRequest(String text) {
        this(text, "IP=");
    }

    AdvancedRequest(String text, String relTypes) {
        this.text = text;
        this.defaultRelTypes = relTypes;

        if (text != null && text.trim().length() > 0) {
	        active = true;
	        try {
	            StringProgress sp = new StringProgress(text);
	            root = parse(sp);
	            sp.ignore(WHITE_SPACE);
	            if (!sp.eof()) {
		            throw new ParseException(sp, "& or | expected");
	            }
	        }
	        catch (ParseException e) {
	            broke=e;
	        }
        }
    }

    Filter parseCombination(StringProgress sp,int level) throws ParseException {
        int next = level + 1;
        BooleanFilter.BooleanCombination combination = BooleanFilter.BooleanCombination.values()[level];
        List<Filter> list = new ArrayList<Filter>();

        while (true) {
            Filter item = (next == BooleanFilter.BooleanCombination.values().length) ? parseItem(sp) : parseCombination(sp, next);
            list.add(item);
            sp.ignore(WHITE_SPACE);
            if (!sp.test(StringProgress.is(combination.op))) {
	            break;
            }
        }

        return BooleanFilter.simplify(combination, list);
    }

    private Filter parseItem(StringProgress sp) throws ParseException {
        sp.ignore(WHITE_SPACE);
        boolean invert = sp.test(StringProgress.is("!"));
        boolean expand = sp.test(StringProgress.is("~"));
        sp.ignore(WHITE_SPACE);
        Filter item;

        if (sp.test(StringProgress.is("*"))) {
            item = new NoFilter();
        }
        else if (sp.test(StringProgress.is("("))) {
            item = parse(sp);
            sp.expect(StringProgress.is(")"), "& or | or ) expected");
        }
        else {
            Like nameValueLike = StringProgress.many(":-.*/", Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.DECIMAL_DIGIT_NUMBER, Character.CONNECTOR_PUNCTUATION);

            List<FieldFilter.FieldName> fields = new ArrayList<FieldFilter.FieldName>();
            List<RelationType> relations = new ArrayList<RelationType>();
            List<String> items = new ArrayList<String>();
            boolean haveFields = false;
            
            while (true) {
                sp.ignore(WHITE_SPACE);
                
                String x = sp.find(nameValueLike);
                
                if (x == null) {
	                throw new ParseException(sp,"Field name or value expected");
                }
                
                items.add(x);
                
                sp.ignore(WHITE_SPACE);
                
                if (sp.test(StringProgress.is("="))) {
                    for (String fieldName : items) {
                        haveFields = true;
                        
                        try {
                            fields.add(FieldFilter.FieldName.valueOf(fieldName));
                        }
                        catch (IllegalArgumentException e) {
                            try {
                                relations.add(RelationType.valueOf(fieldName.toUpperCase()));                                
                            }
                            catch (IllegalArgumentException e2) {
                                throw new ParseException(sp, "Field name expected: " + fieldName + " not one of " + CollectionUtils.concat(FieldFilter.FieldName.values(), ",") + "," + CollectionUtils.concat(RelationType.values(), ","));
                            }
                        }
                    }
                    items.clear();
                }
                else if (!sp.test(StringProgress.is(","))) {
	                break;
                }
            }
                
            List<Filter> inList = new ArrayList<Filter>();

            for (String value : items) {
                if (haveFields) {
                    for (FieldFilter.FieldName fieldName : fields) {
                        if (fieldName == FieldFilter.FieldName.ancestor && value.startsWith("GO:")) {
                            for (int i = 0; i < defaultRelTypes.length(); i++) {
                                inList.add(new FieldFilter(FieldFilter.FieldName.ancestor, defaultRelTypes.charAt(i) + value));
                            }
                        }
                        else {
                            inList.add(new FieldFilter(fieldName, value));
                        }
                    }
                    for (RelationType relationType : relations) {
                        inList.add(new FieldFilter(FieldFilter.FieldName.ancestor, relationType.code + value));
                    }
                }
                else {
                    inList.add(new FieldFilter(null, value));
                }
            }

            item = BooleanFilter.simplify(BooleanFilter.BooleanCombination.or, inList);
        }

        if (expand) {
	        item = new Expand(item);
        }

        if (invert) {
	        item = new NotFilter(item);
        }

        return item;
    }

    private Filter parse(StringProgress sp) throws ParseException {
        Filter filter = parseCombination(sp, 0);
        if (filter == null) {
	        filter = new NoFilter();
        }
        return filter;
    }
}
