"""Data models for Jaeger traces."""

from .trace import Trace
from .span import Span
from .process import Process
from .reference import Reference

__all__ = ['Trace', 'Span', 'Process', 'Reference']
