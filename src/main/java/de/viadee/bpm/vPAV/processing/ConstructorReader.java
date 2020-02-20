/**
 * BSD 3-Clause License
 *
 * Copyright © 2019, viadee Unternehmensberatung AG
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import de.viadee.bpm.vPAV.processing.code.flow.*;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// TODO give it a better name
public class ConstructorReader {

    static ObjectVariable createObjectFromConstructorBlock(final Block block, final List<Value> args) {
        ObjectVariable objVar = new ObjectVariable();
        HashMap<String, StringVariable> localStringVariables = new HashMap<>();

        // Todo Only String variables are currently resolved
        // Eventually add objects, arrays and int

        final Iterator<Unit> unitIt = block.iterator();

        Unit unit;
        while (unitIt.hasNext()) {
            unit = unitIt.next();
            // e. g. r0 := @this: de.viadee.bpm.vPAV.delegates.TestDelegate
            if (unit instanceof IdentityStmt) {
                // IdentityStatement, used to find index of method's arguments and name, if
                // possible (has to be a String)
                if (((IdentityStmt) unit).getRightOp() instanceof ParameterRef) {
                    int idx = ((ParameterRef) ((IdentityStmt) unit).getRightOp()).getIndex();
                    if (args.get(idx).getType().toString().equals("java.lang.String")) {
                        StringVariable var = new StringVariable(((StringConstant) args.get(idx)).value);
                        localStringVariables.put(((JimpleLocal) ((IdentityStmt) unit).getLeftOp()).getName(), var);
                    }
                }
            }
            // e. g. $r2 = staticinvoke ... (Assignment)
            if (unit instanceof AssignStmt) {
                // Local String Variable is updated by directly assigning a new String constant
                if (((AssignStmt) unit).getLeftOpBox().getValue() instanceof JimpleLocal && ((AssignStmt) unit)
                        .getLeftOpBox().getValue()
                        .getType().equals(RefType.v("java.lang.String")) &&
                        ((AssignStmt) unit).getRightOpBox().getValue() instanceof StringConstant
                ) {
                    String varIdentifier = ((AssignStmt) unit).getLeftOpBox().getValue().toString();
                    String value = ((StringConstant) ((AssignStmt) unit).getRightOpBox().getValue()).value;
                    objVar.updateStringField(varIdentifier, value);
                }
                // Object String Variable is updated by referencing another local variable
                else if (((AssignStmt) unit)
                        .getLeftOpBox().getValue()
                        .getType().equals(RefType.v("java.lang.String")) && ((AssignStmt) unit).getRightOpBox()
                        .getValue() instanceof JimpleLocal) {
                    // Extract name
                    int spaceIdx = ((AssignStmt) unit).getLeftOpBox().getValue().toString().lastIndexOf(" ");
                    int gtsIdx = ((AssignStmt) unit).getLeftOpBox().getValue().toString().lastIndexOf(">");
                    String objIdentifier = ((JInstanceFieldRef) ((AssignStmt) unit).getLeftOpBox().getValue()).getBase()
                            .toString();
                    String varName = ((AssignStmt) unit).getLeftOpBox().getValue().toString()
                            .substring(spaceIdx, gtsIdx);
                    String localVar = ((AssignStmt) unit).getRightOpBox().getValue().toString();
                    if (objIdentifier.equals("r0")) {
                        // this object is referenced (ignore all other objects at the moment)
                        objVar.updateStringField(varName, localStringVariables.get(localVar).getValue());
                    }
                }
            }
        }
        return objVar;
    }
}
