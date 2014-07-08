package org.jboss.as.jpa.hibernate4.management;

/**
 * StringReplace
 *
 * @author Scott Marlow
 */
public class StringReplace {

    private final String find;
    private final String replacement;


    public StringReplace(String find, String replacement) {
        this.find = find;
        this.replacement = replacement;
    }

    /**
     * Substitute sub-strings inside of a string.
     * @param input buffer to apply replacements in
     * @param replacements to be made
     *
     */
    public static String replace(String input, StringReplace[] replacements) {
        final StringBuilder stringBuilder = new StringBuilder(input);

        for (StringReplace stringReplace : replacements) {
            int end = 0;
            while ((end = stringBuilder.indexOf(stringReplace.find, end)) != -1) {
                stringBuilder.delete(end, end + stringReplace.find.length());
                stringBuilder.insert(end, stringReplace.replacement);

                // update positions
                end = end + stringReplace.replacement.length();
            }
        }

        return stringBuilder.toString();
    }


}
