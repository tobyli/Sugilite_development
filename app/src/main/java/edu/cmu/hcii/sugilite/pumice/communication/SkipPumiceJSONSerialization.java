package edu.cmu.hcii.sugilite.pumice.communication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author toby
 * @date 2/14/19
 * @time 1:14 PM
 */

/**
 * annotation used to skip objects from serialization when building PumiceInstructionPacket
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SkipPumiceJSONSerialization
{

}
