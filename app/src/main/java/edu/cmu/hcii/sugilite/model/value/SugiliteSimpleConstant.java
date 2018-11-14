package edu.cmu.hcii.sugilite.model.value;

/**
 * @author toby
 * @date 11/14/18
 * @time 12:54 AM
 */
public class SugiliteSimpleConstant<T> implements SugiliteValue<T> {
    T value;
    public SugiliteSimpleConstant(T value){
        this.value = value;
    }
    @Override
    public T evaluate() {
        return value;
    }
}
