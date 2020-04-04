package se.kry.codetest;

public enum Status {
    OK("OK"),FAIL("FAIL"), NOT_TESTED("NOT TESTED");

    private String name = "";

    Status(String name){
        this.name = name;
    }
}
