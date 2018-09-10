package com.centurylink.mdw.tests.cloud

import com.centurylink.mdw.workflow.activity.AbstractEvaluator
import com.centurylink.mdw.annotations.Activity;
import com.centurylink.mdw.activity.types.EvaluatorActivity;

/**
 * MDW evaluator activity.
 */
@Activity(value="Which Activity", category=EvaluatorActivity::class, icon="shape:decision")
class WhichActivity : AbstractEvaluator() {
    override fun evaluate(): Any? {
        return getValue("testCase")
    }
}