#!/usr/bin/env python3

"""
Generate html file with relative links to all existing swagger specs and with specs description
"""

import argparse
import os

from renderer import Renderer


def get_specs_file_names(files) -> list[str]:
    result = []
    for file in files:
        name, extension = os.path.splitext(file)
        if extension == ".yaml":
            result.append(name)
    return result


def make_html(profile) -> None:
    path = os.path.abspath(profile)

    if not os.path.isdir(path):
        raise Exception(f"Folder with swagger specs not found: profile={profile} path={path}")

    files = os.listdir(path)
    files.sort()

    specs_file_names = get_specs_file_names(files)

    renderer = Renderer(specs_file_names, profile)
    index_file_path = f"{profile}/index.html"
    with open(index_file_path, "w") as f:
        f.write(renderer.render())

    abs_path_to_index_file = os.path.abspath(index_file_path)
    print(f"Index file is generated: abs_path_to_index_file={abs_path_to_index_file}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", type=str, default="default",
                        help="profile for which build swagger index.html")
    args = parser.parse_args()

    make_html(args.profile)


if __name__ == "__main__":
    main()
