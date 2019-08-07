import low from '../node/node_modules/lowlight/lib/core';
import css from '../node/node_modules/highlight.js/lib/languages/css';
import excel from '../node/node_modules/highlight.js/lib/languages/excel';
import groovy from '../node/node_modules/highlight.js/lib/languages/groovy';
import java from '../node/node_modules/highlight.js/lib/languages/java';
import javascript from '../node/node_modules/highlight.js/lib/languages/javascript';
import json from '../node/node_modules/highlight.js/lib/languages/json';
import kotlin from '../node/node_modules/highlight.js/lib/languages/kotlin';
import text from '../node/node_modules/highlight.js/lib/languages/plaintext';
import xml from '../node/node_modules/highlight.js/lib/languages/xml';
import yaml from '../node/node_modules/highlight.js/lib/languages/yaml';

low.registerLanguage('css', css);
low.registerLanguage('groovy', groovy);
low.registerLanguage('java', java);
low.registerLanguage('javascript', javascript);
low.registerLanguage('json', json);
low.registerLanguage('kotlin', kotlin);
low.registerLanguage('text', text);
low.registerLanguage('xlsx', excel);
low.registerLanguage('xml', xml);
low.registerLanguage('yaml', yaml);


const languages = {
  supportedLanguages: {
    camel: 'xml',
    css: 'css',
    evth: 'json',
    groovy: 'groovy',
    html: 'html',
    impl: 'json',
    java: 'java',
    js: 'javascript',    
    json: 'json',
    jsx: 'javascript',
    kotlin: 'kotlin',
    kts: 'kotlin',
    pagelet: 'json',
    postman: 'json',
    proc: 'json',
    spring: 'xml',
    task: 'json',
    test: 'groovy',
    text: 'text',
    wsdl: 'xml',
    xlsx: 'excel',
    xsd: 'xml',
    xsl: 'xml',
    yaml: 'yaml'
  },
  getLanguage: function(extension) {
    return this.supportedLanguages[extension] || 'text';
  }
};

export default languages;