#!/usr/bin/env python3
"""Fail-closed publication tests: fake Docker, no registry calls."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class PublishGateTest(unittest.TestCase):
    def run_case(self, *, image_id='sha256:approved', evidence_ref='ghcr.io/example/fixture:test'):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copy(Path(__file__).with_name('03-publish.sh'), root)
            release = root / 'target/release'; release.mkdir(parents=True)
            evidence = release / 'image-evidence.json'
            record = {'imageRef': evidence_ref, 'localImageId': 'sha256:approved',
                      'integrityGate': 'passed', 'vulnerabilityGate': 'not-run', 'registryDigest': None}
            evidence.write_text(json.dumps(record))
            fake = root / 'docker'
            fake.write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
with Path(os.environ['DOCKER_CALLS']).open('a') as log:
    log.write(' '.join(sys.argv[1:]) + '\\n')
if sys.argv[1:3] == ['image', 'inspect']:
    print(os.environ['FAKE_IMAGE_ID']); sys.exit(0)
if sys.argv[1:3] == ['scout', 'cves']:
    sys.exit(2)
raise SystemExit('Unexpected Docker command; external calls are forbidden')
''')
            fake.chmod(0o755)
            log = root / 'calls'
            env = os.environ | {'PATH': f'{root}:{os.environ["PATH"]}', 'DOCKER_CALLS': str(log), 'FAKE_IMAGE_ID': image_id}
            result = subprocess.run(['bash', str(root / '03-publish.sh'), 'ghcr.io/example/fixture:test'], env=env,
                                    capture_output=True, text=True)
            calls = log.read_text() if log.exists() else ''
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn('push ', calls)
            self.assertEqual(json.loads(evidence.read_text()), record)
            return calls

    def test_scanner_failure_never_pushes(self):
        self.assertIn('scout cves --exit-code', self.run_case())

    def test_retagged_image_stops_before_scanner(self):
        self.assertNotIn('scout', self.run_case(image_id='sha256:different'))

    def test_unrelated_evidence_stops_before_docker(self):
        self.assertEqual('', self.run_case(evidence_ref='ghcr.io/another/fixture:test'))


if __name__ == '__main__': unittest.main()
