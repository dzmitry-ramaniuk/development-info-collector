#!/usr/bin/env python3
"""DynamoDB create → conditional writes → Query → cleanup; local by default."""
import argparse
import json
import os
import subprocess
import sys
import uuid
from urllib.parse import urlsplit


class AwsCli:
    def __init__(self, endpoint, region, profile=None):
        self.prefix = [os.environ.get('AWS_CLI_BIN', 'aws'), '--no-cli-pager',
                       '--output', 'json', '--region', region]
        self.env = os.environ.copy()
        if endpoint:
            url = urlsplit(endpoint)
            if url.scheme not in ('http', 'https') or url.hostname not in ('localhost', '127.0.0.1', '::1'):
                raise ValueError('Local endpoint must use loopback; cloud requires explicit --aws')
            self.prefix += ['--endpoint-url', endpoint]
            self.env.update(AWS_ACCESS_KEY_ID='localfake', AWS_SECRET_ACCESS_KEY='localfake',
                            AWS_EC2_METADATA_DISABLED='true')
            self.env.pop('AWS_SESSION_TOKEN', None)
            self.env.pop('AWS_PROFILE', None)
        elif profile:
            self.prefix += ['--profile', profile]

    def __call__(self, *args, expected_error=None):
        result = subprocess.run(self.prefix + ['dynamodb', *args], env=self.env,
                                capture_output=True, text=True, check=False)
        if expected_error:
            if result.returncode == 0 or f'({expected_error})' not in result.stderr:
                raise RuntimeError(f'Expected {expected_error}; got exit {result.returncode}: {result.stderr}')
            return None
        if result.returncode:
            raise RuntimeError(result.stderr.strip() or f'AWS CLI exit {result.returncode}')
        return json.loads(result.stdout) if result.stdout.strip() else {}


def run_lab(cli, table):
    created = False
    try:
        print(f'Creating {table}')
        cli('create-table', '--table-name', table,
            '--attribute-definitions', 'AttributeName=PK,AttributeType=S', 'AttributeName=SK,AttributeType=S',
            '--key-schema', 'AttributeName=PK,KeyType=HASH', 'AttributeName=SK,KeyType=RANGE',
            '--billing-mode', 'PAY_PER_REQUEST')
        created = True
        cli('wait', 'table-exists', '--table-name', table)
        print(f'Created {table}')
        for order_id in ('001', '002'):
            item = {'PK': {'S': 'TENANT#demo'}, 'SK': {'S': f'ORDER#{order_id}'}, 'status': {'S': 'NEW'}}
            cli('put-item', '--table-name', table, '--item', json.dumps(item),
                '--condition-expression', 'attribute_not_exists(PK)')
        duplicate = {'PK': {'S': 'TENANT#demo'}, 'SK': {'S': 'ORDER#001'}, 'status': {'S': 'OVERWRITTEN'}}
        cli('put-item', '--table-name', table, '--item', json.dumps(duplicate),
            '--condition-expression', 'attribute_not_exists(PK)',
            expected_error='ConditionalCheckFailedException')
        print('Duplicate rejected: ConditionalCheckFailedException')
        stored = cli('get-item', '--table-name', table, '--consistent-read', '--key',
                     json.dumps({k: duplicate[k] for k in ('PK', 'SK')}))
        if stored.get('Item', {}).get('status') != {'S': 'NEW'}:
            raise RuntimeError('Conditional write changed existing item')
        result = cli('query', '--table-name', table, '--consistent-read',
                     '--key-condition-expression', 'PK = :pk AND begins_with(SK, :prefix)',
                     '--expression-attribute-values', json.dumps({':pk': {'S': 'TENANT#demo'}, ':prefix': {'S': 'ORDER#'}}))
        if [item['SK']['S'] for item in result.get('Items', [])] != ['ORDER#001', 'ORDER#002']:
            raise RuntimeError('Query did not return both orders in sort-key order')
        print('Read verified: status=NEW; Query returned ORDER#001, ORDER#002')
    finally:
        if created:
            cli('delete-table', '--table-name', table)
            cli('wait', 'table-not-exists', '--table-name', table)
            print(f'Cleanup complete: {table} deleted')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--endpoint', default='http://127.0.0.1:8000')
    parser.add_argument('--aws', action='store_true', help='Create temporary billable resources in your AWS account')
    parser.add_argument('--profile')
    parser.add_argument('--region')
    args = parser.parse_args()
    if args.aws and (not args.profile or not args.region):
        parser.error('--aws requires explicit --profile and --region')
    if not args.aws and args.profile:
        parser.error('--profile is only used with --aws; local uses fake credentials')
    cli = AwsCli(None if args.aws else args.endpoint, args.region or 'us-east-1', args.profile)
    run_lab(cli, 'infra-lab-' + uuid.uuid4().hex[:12])


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, RuntimeError) as error:
        print(f'Lab failed: {error}', file=sys.stderr)
        sys.exit(1)
