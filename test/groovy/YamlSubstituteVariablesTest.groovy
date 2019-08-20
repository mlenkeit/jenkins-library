import com.sap.piper.variablesubstitution.ExecutionContext
import org.junit.Before
import util.JenkinsDeleteFileRule

import static org.junit.Assert.*
import static util.JenkinsWriteYamlRule.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import util.BasePiperTest
import util.JenkinsEnvironmentRule
import util.JenkinsErrorRule
import util.JenkinsLoggingRule
import util.JenkinsReadYamlRule
import util.JenkinsStepRule
import util.JenkinsWriteYamlRule
import util.Rules

import static util.JenkinsWriteYamlRule.DATA
import static util.JenkinsWriteYamlRule.DATA
import static util.JenkinsWriteYamlRule.SERIALIZED_YAML
import static util.JenkinsWriteYamlRule.SERIALIZED_YAML;

public class YamlSubstituteVariablesTest extends BasePiperTest {

    private JenkinsStepRule script = new JenkinsStepRule(this)
    private JenkinsReadYamlRule readYamlRule = new JenkinsReadYamlRule(this)
    private JenkinsWriteYamlRule writeYamlRule = new JenkinsWriteYamlRule(this)
    private JenkinsErrorRule errorRule = new JenkinsErrorRule(this)
    private JenkinsEnvironmentRule environmentRule = new JenkinsEnvironmentRule(this)
    private JenkinsLoggingRule loggingRule = new JenkinsLoggingRule(this)
    private ExpectedException expectedExceptionRule = ExpectedException.none()
    private JenkinsDeleteFileRule deleteFileRule = new JenkinsDeleteFileRule(this)

    @Rule
    public RuleChain rules = Rules
        .getCommonRules(this)
        .around(readYamlRule)
        .around(writeYamlRule)
        .around(errorRule)
        .around(environmentRule)
        .around(loggingRule)
        .around(script)
        .around(deleteFileRule)
        .around(expectedExceptionRule)

    @Before
    public void setup() {
        readYamlRule.registerYaml("manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/manifest.yml")))
                    .registerYaml("manifest-variables.yml", new FileInputStream(new File("test/resources/variableSubstitution/manifest-variables.yml")))
                    .registerYaml("test/resources/variableSubstitution/manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/manifest.yml")))
                    .registerYaml("test/resources/variableSubstitution/manifest-variables.yml", new FileInputStream(new File("test/resources/variableSubstitution/manifest-variables.yml")))
                    .registerYaml("test/resources/variableSubstitution/invalid_manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/invalid_manifest.yml")))
                    .registerYaml("test/resources/variableSubstitution/novars_manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/novars_manifest.yml")))
                    .registerYaml("test/resources/variableSubstitution/multi_manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/multi_manifest.yml")))
                    .registerYaml("test/resources/variableSubstitution/datatypes_manifest.yml", new FileInputStream(new File("test/resources/variableSubstitution/datatypes_manifest.yml")))
                    .registerYaml("test/resources/variableSubstitution/datatypes_manifest-variables.yml", new FileInputStream(new File("test/resources/variableSubstitution/datatypes_manifest-variables.yml")))
    }

    @Test
    public void substituteVariables_Fails_If_InputYamlIsNullOrEmpty() throws Exception {

        expectedExceptionRule.expect(hudson.AbortException)
        expectedExceptionRule.expectMessage("[YamlSubstituteVariables] Input Yaml data must not be null or empty.")

        // execute step
        script.step.yamlSubstituteVariables inputYaml: null, variablesYaml: null, script: nullScript
    }

    @Test
    public void substituteVariables_Fails_If_VariablesYamlIsNullOrEmpty() throws Exception {
        String manifestFileName = "test/resources/variableSubstitution/manifest.yml"

        expectedExceptionRule.expect(hudson.AbortException)
        expectedExceptionRule.expectMessage("[YamlSubstituteVariables] Variables Yaml data must not be null or empty.")

        Object input = script.step.readYaml file: manifestFileName

        // execute step
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: null, script: nullScript
    }

    @Test
    public void substituteVariables_Throws_If_InputYamlIsInvalid() throws Exception {
        String manifestFileName = "test/resources/variableSubstitution/invalid_manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/invalid_manifest.yml"

        //check that exception is thrown and that it has the correct message.
        expectedExceptionRule.expect(org.yaml.snakeyaml.scanner.ScannerException)
        expectedExceptionRule.expectMessage("found character '%' that cannot start any token. (Do not use % for indentation)")

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, script: nullScript
    }

    @Test
    public void substituteVariables_Throws_If_VariablesYamlInvalid() throws Exception {
        String manifestFileName = "test/resources/variableSubstitution/manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/invalid_manifest.yml"

        //check that exception is thrown and that it has the correct message.
        expectedExceptionRule.expect(org.yaml.snakeyaml.scanner.ScannerException)
        expectedExceptionRule.expectMessage("found character '%' that cannot start any token. (Do not use % for indentation)")

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, script: nullScript
    }

    @Test
    public void substituteVariables_ReplacesVariablesProperly_InSingleYamlFiles() throws Exception {
        String manifestFileName = "test/resources/variableSubstitution/manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/manifest-variables.yml"

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, script: nullScript
        Map<String, Object> manifestDataAfterReplacement = environmentRule.env.getValue('yamlSubstituteVariablesResult')

        //Check that something was written
        assertNotNull(manifestDataAfterReplacement)

        // check that resolved variables have expected values
        assertCorrectVariableResolution(manifestDataAfterReplacement)

        // check that the step was marked as a success (even if it did do nothing).
        assertJobStatusSuccess()
    }

    private void assertAllVariablesReplaced(String yamlStringAfterReplacement) {
        assertFalse(yamlStringAfterReplacement.contains("(("))
        assertFalse(yamlStringAfterReplacement.contains("))"))
    }

    private void assertCorrectVariableResolution(Map<String, Object> manifestDataAfterReplacement) {
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("name").equals("uniquePrefix-catalog-service-odatav2-0.0.1"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("routes").get(0).get("route").equals("uniquePrefix-catalog-service-odatav2-001.cfapps.eu10.hana.ondemand.com"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("services").get(0).equals("uniquePrefix-catalog-service-odatav2-xsuaa"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("services").get(1).equals("uniquePrefix-catalog-service-odatav2-hana"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("xsuaa-instance-name").equals("uniquePrefix-catalog-service-odatav2-xsuaa"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("db_service_instance_name").equals("uniquePrefix-catalog-service-odatav2-hana"))
    }

    @Test
    public void substituteVariables_ReplacesVariablesProperly_InMultiYamlData() throws Exception {
        String manifestFileName = "test/resources/variableSubstitution/multi_manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/manifest-variables.yml"

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, script: nullScript
        List<Object> manifestDataAfterReplacement = environmentRule.env.getValue('yamlSubstituteVariablesResult')

        //Check that something was written
        assertNotNull(manifestDataAfterReplacement)

        //check that result still is a multi-YAML file.
        assertEquals("Dumped YAML after replacement should still be a multi-YAML file.",2, manifestDataAfterReplacement.size())

        // check that resolved variables have expected values
        manifestDataAfterReplacement.each { yaml ->
            assertCorrectVariableResolution(yaml as Map<String, Object>)
        }

        // check that the step was marked as a success (even if it did do nothing).
        assertJobStatusSuccess()
    }

    @Test
    public void substituteVariables_ReturnsOriginalIfNoVariablesPresent() throws Exception {
        // This test makes sure that, if no variables are found in a manifest that need
        // to be replaced, the execution is eventually skipped and the manifest remains
        // untouched.

        String manifestFileName = "test/resources/variableSubstitution/novars_manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/manifest-variables.yml"

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        ExecutionContext context = new ExecutionContext()
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, executionContext: context, script: nullScript
        Object result = environmentRule.env.getValue('yamlSubstituteVariablesResult')

        //Check that nothing was written
        assertNotNull(result)
        assertTrue(context.noVariablesReplaced)

        // check that the step was marked as a success (even if it did do nothing).
        assertJobStatusSuccess()
    }

    @Test
    public void substituteVariables_SupportsAllDataTypes() throws Exception {
        // This test makes sure that, all datatypes supported by YAML are also
        // properly substituted by the substituteVariables step.
        // In particular this includes variables of type:
        // Integer, Boolean, String, Float and inline JSON documents (which are parsed as multi-line strings)
        // and complex types (like other YAML objects).
        // The test also checks the differing behaviour when substituting nodes that only consist of a
        // variable reference and nodes that contains several variable references or additional string constants.

        String manifestFileName = "test/resources/variableSubstitution/datatypes_manifest.yml"
        String variablesFileName = "test/resources/variableSubstitution/datatypes_manifest-variables.yml"

        Object input = script.step.readYaml file: manifestFileName
        Object variables = script.step.readYaml file: variablesFileName

        // execute step
        ExecutionContext context = new ExecutionContext()
        script.step.yamlSubstituteVariables inputYaml: input, variablesYaml: variables, executionContext: context, script: nullScript
        Map<String, Object> manifestDataAfterReplacement = environmentRule.env.getValue('yamlSubstituteVariablesResult')

        //Check that something was written
        assertNotNull(manifestDataAfterReplacement)

        assertCorrectVariableResolution(manifestDataAfterReplacement)

        assertDataTypeAndSubstitutionCorrectness(manifestDataAfterReplacement)

        // check that the step was marked as a success (even if it did do nothing).
        assertJobStatusSuccess()
    }

    private void assertDataTypeAndSubstitutionCorrectness(Map<String, Object> manifestDataAfterReplacement) {
        // See datatypes_manifest.yml and datatypes_manifest-variables.yml.
        // Note: For debugging consider turning on YAML writing to a file in JenkinsWriteYamlRule to see the
        // actual outcome of replacing variables (for visual inspection).

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("instances").equals(1))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("instances") instanceof Integer)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("services").get(0) instanceof String)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("booleanVariable").equals(true))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("booleanVariable") instanceof Boolean)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("floatVariable") == 0.25)
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("floatVariable") instanceof Double)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("json-variable") instanceof String)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("object-variable") instanceof Map)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("string-variable").startsWith("true-0.25-1-"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("string-variable") instanceof String)

        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("single-var-with-string-constants").equals("true-with-some-more-text"))
        assertTrue(manifestDataAfterReplacement.get("applications").get(0).get("env").get("single-var-with-string-constants") instanceof String)
    }
}
