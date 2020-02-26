package de.viadee.bpm.vPAV.processing;

public class SimpleObject {

    private String myStringField;

    public SimpleObject() {
        myStringField = "hello";
        method();
        String var = methodWithReturn();
    }

    private void method() {
        myStringField = "bye";
    }

    private String methodWithReturn() {
        return "it_works";
    }
}
