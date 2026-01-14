class JmLocal < Formula

	version "0.0"
	desc "jMolecules CLI - Local formula"
	url "file:///dev/null"
	sha256 "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

	depends_on "jq"
	
	def install

		bin.mkpath
		cp "#{__dir__}/../bin/jm", bin/"jm"

		bash_completion.mkpath
    	cp "#{__dir__}/../completion/jm.completion", bash_completion/"jm"
	end
end
