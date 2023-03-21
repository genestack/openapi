# Swagger API specification in yaml format

## Prerequisites
Install python dependencies:
````
pip install -r requirements.txt
````
## How to generate swagger index.html
Swagger index.html - starting page of our swagger, you could see how it currently looks here [`./default/index.html`]

Swagger index.html has to be regenerated if new section is added or section descriptions are changed. Section 
descriptions are here [`./section_descriptions.json`]

The index.html will be generated based on 
manually written yaml specs from [`./default`] or [`./inc`], [`./section_descriptions.json`] and [Jinja templates]
- INC profile
````
python make_swagger_html.py --profile inc
````
- Default profile
````
python make_swagger_html.py --profile default
````

[`./default/index.html`]: ./default/index.html
[`./default`]: ./default
[`./inc`]: ./inc
[`./section_descriptions.json`]: ./section_descriptions.json
[Jinja templates]: ./templates
