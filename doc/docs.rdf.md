<!-- Use nonstandard prefixes merely to exercise prefix feature for
tests -->

<?prefix :arachne.core=http://arachne-framework.org/name/ ?>
<?prefix :a=urn:arachne:?>

<?about :arachne.core/arachne-doc ?>

This module provides built-in documentation support for Arachne. When
the module is included in a project, it will scan the classpath for
any files named `*.rdf.edn`, and parse them into markdown
snippets. These snippets will then included as RDF in the project's
descriptor under the <?ref :arachne/doc ?> attribute.

TODO: Write docs for parsing and snippet format.

<?about :a/doc ?>

Associates any RDF Resource with Markdown documentation.
