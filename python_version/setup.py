"""Setup script for Jaeger UML Generator."""

from setuptools import setup, find_packages
from pathlib import Path

# Read the README file
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text(encoding='utf-8') if (this_directory / "README.md").exists() else ""

setup(
    name="jaeger-uml-generator",
    version="1.0.0",
    author="UML Generator Project",
    description="Generate UML diagrams in XMI format from Jaeger distributed traces",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/yourusername/jaeger-uml-generator",
    packages=find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Code Generators",
    ],
    python_requires=">=3.9",
    install_requires=[
        "requests>=2.31.0",
    ],
    entry_points={
        "console_scripts": [
            "jaeger-uml-generator=jaeger_uml_generator.main:main",
        ],
    },
)
