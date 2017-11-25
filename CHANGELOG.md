# Changelog

## v0.3

- Use oasp.json to replace url.txt to carry richer information. OASP url can be one of the fields in this json.
- OASP version (in which OASP scheme the app was signed) is added as a mandatory field in oasp.json and the POST request to the remote server.

## v0.2

- Avoid the duplication of the MF/SF digest files introduced by the OASP signature. Now OASP only put its certificate and url under APK signing; it then signs the MANIFEST.MF (shared with APK signing) and inserts the OASP signature into META-INF. This significantly reduces file size increase, simplifies the signing/verification process, and speeds up the performance.

- Split the OASP verification logic out of the demo app as a standalone module, which makes it easier for other apps to integrate OASP.

- Further polish the tool scripts and test cases.

- Unify the term "IDSIG" and "OASP" into "OASP" alone to avoid confusion.

## v0.1

- The initial implementation of the nested signing scheme described in [our MOSEC 2017 talk](docs/mosec17.pdf)