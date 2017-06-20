# Description of Test Cases

## Client-side Tests

### client.apk

This is a compiled version of the OASP client app provided in this repo.

### test1.apk

This is an apk generated from the sign.sh script provided in this repo. It has a valid apk signature and a valid IDSIG signature. It can pass the IDSIG verification of the OASP client app provided in this repo.

### test2.apk

This is an apk with tampered IDSIG signature but valid apk signature. It can pass Android's apk verification (so it can be installed), but cannot pass the OASP client app's IDSIG verification.

### test3.apk

This is an apk with a valid apk signature and a valid IDSIG signature. It also has a valid IDSIG_OLD signature. It can pass the IDSIG verification of the OASP client app, and can replace an old version that used IDSIG_OLD cert to generate the IDSIG signature. Note that the replacement/upgrade logic does not belong to the OASP client app; it is up to the device vendors to implement such logic into the Android framework. In the future we will provide a reference patch to the Android framework.

### test4.apk

This is an apk with a valid apk signature and a valid IDSIG signature. However, it has an invalid IDSIG_OLD signature. It cannot pass the IDSIG verification process of the OASP client. This mimics the situation where an attacker wants to update an App signed by IDSIG_OLD to a malicious app signed by IDSIG, without actually being the owner of IDSIG_OLD


## Server-side Tests

Here are some curl commands to test the server functions. If you are testing a server without valid SSL certificate, remember to add "-k/--insecure" to the curl commands.

### GET

```
curl https://[OASP_SERVER]
```

The server should return the supported OASP protocol version, like '{"oasp_version":1}'.

### POST

```
curl -X POST -d '{ "pkg":"com.yulong.app", "ver":1, "hash":"C316183D78D89BFD2900E20FA90AB152AF21F89F0BF5C75749E14E743724C51F", "cert":"6951C2D4BCB3457FC50314D4D05D69DD72E90EDCB06CDE1D9EB7D09D413F4B4C", "idsig":"CF802C91235D125EF0BF311BD3A0CDD4EDCB8BEB4F1DED5A6E0A05A98D77047A"}' https://[OASP_SERVER]
```

The server should return the OASP status code, like '{"oasp_result":1}'.

You can try to remove some field or modify some field to invalid values, then the server should return '{"oasp_result":-3}'.

The current defined OASP status codes are:
* 0: Redirect
* 1: OK
* -1: Bad
* -2: Unknown
* -3: Invalid
