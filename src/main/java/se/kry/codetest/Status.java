package se.kry.codetest;

public enum Status {
    OK("OK"),FAIL("FAIL");

    private String name = "";

    Status(String name){
        this.name = name;
    }
}
