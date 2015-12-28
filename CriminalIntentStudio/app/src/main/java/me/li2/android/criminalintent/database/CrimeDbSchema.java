package me.li2.android.criminalintent.database;

/**
 * Created by weiyi on 15/12/28.
 */
public class CrimeDbSchema {

    // Only to describe the constant string(name, columns) to describe the table
    // refer to column named "title" in this way: CrimeTable.Cols.TITLE
    public static final class CrimeTable {
        public static final String NAME = "crimes";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String SOLVED = "solved";
        }
    }
}
