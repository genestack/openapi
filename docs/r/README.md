## How to generate R Client documentation

* Generate R client. Compiling always removes generated code. Client code will be generated if flag specified
* From the directory of `client` R package run the following commands:

```bash
R -e "devtools::document()"
R CMD Rd2pdf ./ --output=manual.pdf --no-index --no-preview --force
```
