#!/usr/bin/env python3
###############################################################################
#                                                                             #
# Copyright (c) 2017 Baidu Inc. All Rights Reserved                           #
#                                                                             #
###############################################################################
"""
This is a demo HTTPS server, responding to OASP requests
Author:  Yulong Zhang (ylzhang@baidu.com)
"""

from http.server import BaseHTTPRequestHandler
from http.server import HTTPServer
import ssl
import json
import re

def validPkg(pkg):
    """ 
    This helper function validates Android package name.
    It returns true if the input is a validate Android package name, 
        or false otherwise.
    For details about the rule, please refer to https://developer.android.com/
        guide/topics/manifest/manifest-element.html#package
    """
    if pkg is None or not isinstance(pkg, str):
        return False

    pkgRegex = "^([A-Za-z]{1}[A-Za-z\d_]*\.)*[A-Za-z][A-Za-z\d_]*$"
    prog = re.compile(pkgRegex)
    if prog.match(pkg):
        return True
    else:
        return False


def validVer(ver):
    """
    This helper function validates Android version code.
    It must be an integer but Google didn't say it must be positive only:
    https://developer.android.com/guide/topics/manifest/
        manifest-element.html#vcode
    """
    if ver is None or not isinstance(ver, int):
        return False
    else:
        return True


def validSha256(sha256):
    """
    This helper function validates SHA-256 hash string.
    """
    if sha256 is None or not isinstance(sha256, str):
        return False

    sha256Regex = "[A-Fa-f0-9]{64}"
    prog = re.compile(sha256Regex)
    if prog.match(sha256):
        return True
    else:
        return False


class Receiver(BaseHTTPRequestHandler):
    """ This class is the actual handler of OASP requests """
    def do_GET(self):
        """ 
        GET handler =>
            It should reply with supported OASP version scheme.
        """
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(bytes('{"oasp_version":1}', 'UTF-8'))

    def do_POST(self):
        """ 
        POST handler =>
            Input: App's pkg name, ver code, SHA256, apk certificate, IDSIG 
            Output: OASP status code
        OASP Status Code:
            0: Redirect
            1: OK
           -1: Bad
           -2: Unknown
           -3: Invalid
        """
        lenth = int(self.headers['Content-Length'])
        payload = self.rfile.read(lenth).decode('UTF-8')
        try:
            req = json.loads(payload)
        except:
            self.send_response(400)
            return

        # Validation of the query
        try:
            if not validPkg(req["pkg"]) or not validVer(req["ver"]) \
                    or not validSha256(req["mf_hash"]) \
                    or not validSha256(req["apk_cert"]) \
                    or not validSha256(req["oasp_cert"]):
                self.wfile.write(bytes('{"oasp_result":-3}', 'UTF-8'))
                return
        except:
            # If the query misses some field, we return Invalid too:
            self.wfile.write(bytes('{"oasp_result":-3}', 'UTF-8'))
            return

        print("Package:\t" + req["pkg"])
        print("Version:\t" + str(req["ver"]))
        print("SHA256:\t\t" + req["mf_hash"])
        print("Certificate:\t" + req["apk_cert"])
        print("IDSIG:\t\t" + req["oasp_cert"])
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()

        # Apply your policy here to determine the App Status
        self.wfile.write(bytes('{"oasp_result":1}', 'UTF-8'))


server_address = ('', 443)
httpd = HTTPServer(server_address, Receiver)
httpd.socket = ssl.wrap_socket(httpd.socket, keyfile="key.pem", 
        certfile='cert.pem', server_side=True)
print('OASP Server Started...')
httpd.serve_forever()