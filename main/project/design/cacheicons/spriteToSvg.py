#!/usr/bin/python3
import os
import re
import glob
import lxml.etree

ns = { 'svg': 'http://www.w3.org/2000/svg' }

dirname = os.path.dirname(__file__)
outdir = os.path.join(dirname, 'svgs')

for f in glob.glob("*.svg"):
    if 'marker_' in f:
        continue
    if 'Disabled' in f:
        suffix = '_disabled'
    elif 'types.svg' == f:
        suffix = '_nobackground'
    else:
        suffix = ''
    infile = os.path.join(dirname, f)
    tree = lxml.etree.parse(infile, parser=lxml.etree.XMLParser(remove_comments=True))
    root = tree.getroot()
    #for bg in tree.xpath("//svg:circle[@id='background']", namespaces={"svg": "http://www.w3.org/2000/svg"}):
    #    bg.getparent().remove(bg)
    #for bg in tree.xpath("//svg:circle[@id='foreground']", namespaces={"svg": "http://www.w3.org/2000/svg"}):
    #    bg.getparent().remove(bg)
    for symbol in root:
        name = symbol.attrib['id']
        m = re.search(r'\d$', name)
        if m is not None:
            continue
        f = open(os.path.join(outdir, name+suffix+'.svg'), 'w')
        svg = lxml.etree.tostring(symbol, encoding='unicode', pretty_print=True)
        svg = svg.replace('symbol', 'svg')
        f.write(svg)
        f.close()