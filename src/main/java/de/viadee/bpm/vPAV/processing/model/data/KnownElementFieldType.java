/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing.model.data;

/**
 * known element field types
 */
public enum KnownElementFieldType {
  Class("Class"), FormField("Form Field"), Expression("Expression"), DelegateExpression(
      "Delegate Expression"), ResultVariable("Result Variable"), CalledElement(
          "Called Element"), CaseRef("Case Ref"), CollectionElement("Collection"), ElementVariable(
              "Element Variable"), LoopCardinality("Loop Cardinality"), CompletionCondition(
                  "Completion Condition"), InlineScript("Inline Script"), ExternalScript(
                      "External Script"), Assignee("Assignee"), CandidateUsers(
                          "Candidate Users"), CandidateGroups("Candidate Groups"), DueDate(
                              "Due Date"), FollowUpDate("Follow Up Date"), DMN(
                                  "DMN"), CamundaIn("Camunda:In"), CamundaOut("Camunda:Out");

  private String description;

  private KnownElementFieldType(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}