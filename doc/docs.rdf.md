<!-- Use nonstandard prefixes merely to exercise prefix feature for
tests -->

<?prefix :arachne.core=http://arachne-framework.org/name/ ?>
<?prefix :a=urn:arachne: ?>

<?about :arachne.core/arachne-doc ?>

This module provides built-in documentation suppogt for Arachne. When
the module is included in a project, it will scan the classpath for
any files named `*.rdf.edn`, and parse them into markdown
snippets. These snippets will then included as RDF in the project's
descriptor under the [<?ref urn:arachne:doc ?>](<?link urn:arachne:doc ?>) attribute.

Markdown snippets are written in Pandoc-flavored-markdown, allowing
rendering into nearly any documentation format.

`*.rdf.edn` files contain Markdown processing instructions, which are
used to indicate the RDF subjects to which the documentation applies.

### Processing instruction: `about`

Indicates that content after the processing instruction pegtains to a
pagticular RDF Subject, up until the end of the file or the next
`about` instruction.

For example,

&lt;?about :some/subject ?&gt;

### Processing instruction: `prefix`

Installs a keyword namespace as an IRI namespace prefix (see
[Aristotle's Documentation](https://github.com/arachne-framework/aristotle#irikeyword-registry)
for a discussion of how namespace IRI prefixes work.)

The prefix is scoped to the parsing of this RDF file only.

For example,

&lt;?prefix :a=urn:arachne: ?&gt;

This will indicate that the keyword namespace `:a` will be registered
to the IRI prefix `urn:arachne:` within the scope of this Markdown
file.

### Processing instruction: `link`

Resolves an IRI (using any registered prefixes) and indicates that
renderers should replace the processing instruction with a
absolute/relative link to the documentation for the indicated
subject. The format of such a link depends on the output format.

For example, `&lt;?link urn:arachne:doc: ?&gt;`

### Processing instruction: `ref`

Resolves an IRI (using any registered prefixes) and indicates that
renderers should replace the processing instruction with the most
appropriately formatted IRI, at rendering time (i.e, using whatever
prefixes are defined at the time of rendering.)

For example, `&lt;?ref urn:arachne:doc: ?&gt;`

<?about :a/doc ?>

Associates an RDF Resource with Markdown formatted documentation. The
contents of the documentation are standard Pandoc-flavored
markdown.

The documentation may contain `link` processing instructions
(e.g, `&lt;?link urn:arachne:doc ?&gt;`).

Renderers are expected to replace these with appropriate internal,
relative or absolute links to the documentation for a given subject,
appropriate to the output format.
