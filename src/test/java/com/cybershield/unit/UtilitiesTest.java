package com.cybershield.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.cybershield.util.Texts;
import com.cybershield.util.VersionComparator;
import com.cybershield.validation.IpAddressValidator;
import org.junit.jupiter.api.Test;

class UtilitiesTest {

    @Test
    void versionComparisonDetectsOutdatedSoftware() {
        assertThat(VersionComparator.isOutdated("8.9", "9.6")).isTrue();
        assertThat(VersionComparator.isOutdated("2.4.52", "2.4.62")).isTrue();
        assertThat(VersionComparator.isOutdated("2.4.62", "2.4.62")).isFalse();
        assertThat(VersionComparator.isOutdated("21.0.5", "21.0.5")).isFalse();
        assertThat(VersionComparator.isOutdated("130.0", "118.0")).isFalse();
        assertThat(VersionComparator.isOutdated("1.0", null)).isFalse();
    }

    @Test
    void ipAddressValidation() {
        assertThat(IpAddressValidator.isValidAddress("10.10.4.104")).isTrue();
        assertThat(IpAddressValidator.isValidAddress("")).isTrue();
        assertThat(IpAddressValidator.isValidAddress("fe80::1")).isTrue();
        assertThat(IpAddressValidator.isValidAddress("2001:db8:0:0:0:0:2:1")).isTrue();
        assertThat(IpAddressValidator.isValidAddress("::ffff:10.0.0.1")).isTrue();
        assertThat(IpAddressValidator.isValidAddress("256.1.1.1")).isFalse();
        assertThat(IpAddressValidator.isValidAddress("10.10.4")).isFalse();
        assertThat(IpAddressValidator.isValidAddress("server01")).isFalse();
        assertThat(IpAddressValidator.isValidAddress("1::2::3")).isFalse();
    }

    @Test
    void textHelpers() {
        assertThat(Texts.clean("  ")).isNull();
        assertThat(Texts.clean(" a ")).isEqualTo("a");
        assertThat(Texts.initials("Ananya Rao")).isEqualTo("AR");
        assertThat(Texts.truncate("abcdefghij", 6)).isEqualTo("abc...");
    }
}
