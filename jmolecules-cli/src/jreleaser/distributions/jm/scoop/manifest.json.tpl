{
    "version": "{{projectVersion}}",
    "description": "{{projectDescription}}",
    "homepage": "{{projectLinkHomepage}}",
    "license": "{{projectLicense}}",
    "url": "{{distributionUrl}}",
    "hash": "sha256:{{distributionChecksumSha256}}",
    "bin": "bin\\{{distributionExecutableName}}.exe",
    "checkver": {
        "github": "{{projectLinkHomepage}}"
    },
    "autoupdate": {
        "url": "{{scoopAutoupdateUrl}}",
        "hash": {
            "url": "$url.sha256"
        }
    }
}
