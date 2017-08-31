// custom glue beyond default mdw steps.groovy

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

Then(~'^my custom outcome should be (.*)') { customOutcome ->
    assert customOutcome == 'GOOD'
}

Then(~'^my other outcome should be (.*)') { otherOutcome ->
    assert otherOutcome == 'OKAY'
}