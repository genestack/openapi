## How to generate Python client documentation

* Generate Python client. Compiling always removes generated code.
  Client code will be generated if flag specified.
* Run `generate_documentation.sh` with parameters to generate sphinx
  documentation:
   - application (`integration-user`)
   - package name (`integration`)

## Other approaches to generate Client documentation

### Use Sphinx to generate documentation directly from swagger .md files
This can be achieved with appropriate extension for swagger like `m2r`
(https://github.com/miyakogi/m2r).

__Advantages__
* No need to create any additional files, just copy swagger `README.md` file
  and _docs/_ directory into sphinx folder (like `path/to/<module>/source/`)
  and add `README` the `index.rst` `.. toctree::` section.

__Disadvantage__
* Can't be packed into single html.
* Links to class methods are broken (can navigate only to another .md file).
* Swagger `.md` files miss linebreaks from `ISwaggerLiterals.java`.
  Sometimes it leads to incorrectly displayed sections.

### Use pydoc
Pydoc is another python documentation tools.

__Advantages__
* It is easier to use.

__Disadvantage__
* Can't be packed into single html.
* Looks outworn compared with sphinx.
