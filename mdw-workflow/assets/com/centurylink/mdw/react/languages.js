import low from '../node/node_modules/lowlight/lib/core';
import java from '../node/node_modules/highlight.js/lib/languages/java';
import javascript from '../node/node_modules/highlight.js/lib/languages/javascript';
import json from '../node/node_modules/highlight.js/lib/languages/json';
import yaml from '../node/node_modules/highlight.js/lib/languages/yaml';
import groovy from '../node/node_modules/highlight.js/lib/languages/groovy';
import kotlin from '../node/node_modules/highlight.js/lib/languages/kotlin';
import css from '../node/node_modules/highlight.js/lib/languages/css';

low.registerLanguage('java', java);
low.registerLanguage('javascript', javascript);
low.registerLanguage('json', json);
low.registerLanguage('yaml', yaml);
low.registerLanguage('groovy', groovy);
low.registerLanguage('kotlin', kotlin);
low.registerLanguage('css', css);

const languages = {
  getLanguage: function(extension) {
    let lang = extension;
    if (lang === 'js') {
      lang = 'javascript';
    }
    else if (lang === 'test') {
      lang = 'groovy';
    }
    else if (lang === 'postman') {
      lang = 'json';
    }
    else if (lang === 'spring' || lang === 'camel') {
      lang = 'xml';
    }
    else if (lang === 'proc' || lang === 'task' || lang === 'impl' || lang === 'evth' || lang == 'pagelet') {
      lang = 'json';
    }
    else if (lang === 'kts') {
      lang = 'kotlin';
    }
    else if (lang === 'jsx') {
      lang = 'javascript';
    }
    return lang;
  }
};

export default languages;