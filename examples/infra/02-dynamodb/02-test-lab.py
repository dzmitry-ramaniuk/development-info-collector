#!/usr/bin/env python3
"""Offline: cleanup and fail-closed negative checks, no AWS calls."""
import importlib.util
import json
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('lab', Path(__file__).with_name('01-lab.py'))
lab = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lab)


class FakeDynamo:
    def __init__(self, fail_at=None):
        self.calls = []
        self.fail_at = fail_at

    def __call__(self, *args, **kwargs):
        self.calls.append(args)
        if args[0] == self.fail_at:
            raise RuntimeError('simulated failure')
        if args[0] == 'get-item':
            return {'Item': {'status': {'S': 'NEW'}}}
        if args[0] == 'query':
            return {'Items': [{'SK': {'S': 'ORDER#001'}}, {'SK': {'S': 'ORDER#002'}}]}
        return {}


class LabTest(unittest.TestCase):
    def test_cleanup_after_success(self):
        cli = FakeDynamo(); lab.run_lab(cli, 'test')
        self.assertEqual(cli.calls[-2:], [('delete-table', '--table-name', 'test'), ('wait', 'table-not-exists', '--table-name', 'test')])

    def test_cleanup_after_processing_failure(self):
        cli = FakeDynamo('query')
        with self.assertRaisesRegex(RuntimeError, 'simulated'):
            lab.run_lab(cli, 'test')
        self.assertEqual(cli.calls[-2][0], 'delete-table')

    def test_does_not_delete_when_create_failed(self):
        cli = FakeDynamo('create-table')
        with self.assertRaises(RuntimeError): lab.run_lab(cli, 'test')
        self.assertNotIn('delete-table', [args[0] for args in cli.calls])

    @patch('subprocess.run')
    def test_access_denied_is_not_an_expected_duplicate(self, run):
        run.return_value = subprocess.CompletedProcess([], 254, '', 'An error occurred (AccessDeniedException)')
        with self.assertRaisesRegex(RuntimeError, 'ConditionalCheckFailedException'):
            lab.AwsCli('http://127.0.0.1:8000', 'us-east-1')('put-item', expected_error='ConditionalCheckFailedException')

    @patch('subprocess.run')
    def test_duplicate_succeeding_is_a_failure(self, run):
        run.return_value = subprocess.CompletedProcess([], 0, '{}', '')
        with self.assertRaises(RuntimeError):
            lab.AwsCli('http://127.0.0.1:8000', 'us-east-1')('put-item', expected_error='ConditionalCheckFailedException')

    def test_local_rejects_remote_endpoint(self):
        with self.assertRaises(ValueError): lab.AwsCli('https://dynamodb.eu-central-1.amazonaws.com', 'eu-central-1')


if __name__ == '__main__': unittest.main()
