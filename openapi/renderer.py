import typing

from jinja2 import Environment, loaders
import json


def _remove_specs_descr_from_sections(section_descriptions, section_identifiers_from_descr, specs_file_names):
    sections2remove = section_identifiers_from_descr - specs_file_names
    print(f"Section that need to be removed: sections2remove={sections2remove}")
    for group, specs2descriptions in section_descriptions.items():
        for key in sections2remove:
            if key in specs2descriptions:
                del specs2descriptions[key]
                print(f'Section removed: group="{group}" spec={key}')


class Renderer:
    def __init__(self, specs_file_names: typing.List[str], profile: str):
        self._specs_file_names = specs_file_names
        self._profile = profile

    @staticmethod
    def _read_section_descriptions():
        with open('section_descriptions.json', 'r') as section_file:
            return json.loads(section_file.read())

    @staticmethod
    def _check_all_specs_have_description(specs_file_names, specs_with_descr):
        specs_without_description = specs_file_names - specs_with_descr

        if len(specs_without_description) > 0:
            raise Exception(
                f"There are specs without description: specs_without_description={specs_without_description}"
            )

    def _get_section_description_for_specs(self):
        section_descriptions = self._read_section_descriptions()
        specs_file_names = set(self._specs_file_names)
        section_identifiers_from_descr = set(
            [key for section in section_descriptions.values() for key in section.keys()]
        )
        self._check_all_specs_have_description(specs_file_names, section_identifiers_from_descr)
        _remove_specs_descr_from_sections(section_descriptions, section_identifiers_from_descr, specs_file_names)

        return section_descriptions

    def render(self) -> str:
        section_descriptions = self._get_section_description_for_specs()
        env = Environment(loader=loaders.FileSystemLoader("templates"))
        template_file_name = self._profile.lower() + ".jinja"
        tmpl = env.get_template(template_file_name)

        return tmpl.render(sections=section_descriptions)
