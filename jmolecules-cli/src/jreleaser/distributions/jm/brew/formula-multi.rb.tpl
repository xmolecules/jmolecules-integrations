class {{brewFormulaName}} < Formula
  desc "{{projectDescription}}"
  homepage "{{projectWebsite}}"
  version "{{projectVersion}}"
  license "{{projectLicense}}"

  {{#brewHasLivecheck}}
  livecheck do
    {{#brewLivecheck}}
    {{{.}}}
    {{/brewLivecheck}}
  end
  {{/brewHasLivecheck}}
  {{#brewDependencies}}
  depends_on {{.}}
  {{/brewDependencies}}

  on_intel do
    url "{{distributionUrl}}"
    sha256 "{{distributionChecksumSha256}}"
  end

  on_arm do
    url "{{distributionUrlArm}}"
    sha256 "{{distributionChecksumSha256Arm}}"
  end

  def install
    libexec.install Dir["*"]
    bin.install_symlink libexec/"bin/{{distributionExecutableName}}"
    bash_completion.install libexec/"completion/{{distributionExecutableName}}.completion" => "{{distributionExecutableName}}"
  end

  test do
    output = shell_output("#{bin}/{{distributionExecutableName}} --version 2>&1")
    assert_match "{{projectVersion}}", output
  end
end
