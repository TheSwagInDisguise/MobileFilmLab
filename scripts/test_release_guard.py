#!/usr/bin/env python3
"""The beta owner approval must not silently authorise another beta or stable release."""
import unittest
from verify_release import verify_approval


class ReleaseApprovalTest(unittest.TestCase):
    def test_alpha_can_remain_a_prerelease(self):
        verify_approval('v0.1.0-alpha.9', None)

    def test_non_alpha_is_blocked_by_default(self):
        for tag in ('v0.2.0-beta.1', 'v0.2.0-rc.1', 'v0.2.0'):
            with self.subTest(tag=tag), self.assertRaises(AssertionError):
                verify_approval(tag, None)

    def test_exact_beta_approval_works(self):
        verify_approval('v0.2.0-beta.1', 'v0.2.0-beta.1')

    def test_beta_approval_does_not_authorise_another_beta(self):
        with self.assertRaises(AssertionError):
            verify_approval('v0.2.0-beta.2', 'v0.2.0-beta.1')

    def test_beta_approval_does_not_authorise_stable(self):
        with self.assertRaises(AssertionError):
            verify_approval('v0.2.0', 'v0.2.0-beta.1')

    def test_old_blanket_boolean_does_not_bypass_exact_tag(self):
        with self.assertRaises(AssertionError):
            verify_approval('v0.2.0-beta.1', 'true')


if __name__ == '__main__':
    unittest.main()
