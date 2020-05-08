import sys
import imp
import os.path

# Finds modules in .py assets
class ModuleFinder(object):

    def find_module(self, fullname, path=None):
        if self.is_asset_subdir(fullname) or self.is_py_asset(fullname):
            return self
        else:
            return None

    def load_module(self, fullname):
        if fullname in sys.modules:
          return None

        if self.is_py_asset(fullname):
            mod = sys.modules.setdefault(fullname, imp.new_module(fullname))
            mod.__file__ = "<%s>" % self.__class__.__name__
            mod.__loader__ = self
            mod.__package__ = fullname.rpartition('.')[0]
            with open(self.get_asset_path(fullname) + '.py', 'r') as reader:
                exec(reader.read(), mod.__dict__)
            return mod
        elif self.is_asset_subdir(fullname):
            mod = sys.modules.setdefault(fullname, imp.new_module(fullname))
            mod.__file__ = "<%s>" % self.__class__.__name__
            mod.__loader__ = self
            mod.__path__ = []
            mod.__package__ = fullname
            return mod
        else:
            return None

    def get_asset_path(self, fullname):
        return assetRoot + '/' + fullname.replace('.', '/')

    def is_asset_subdir(self, fullname):
        return os.path.isdir(self.get_asset_path(fullname))

    def is_package_dir(self, dirpath):
        return os.path.isfile(dirpath + '/.mdw/package.yaml')

    def is_py_asset(self, fullname):
        filepath = self.get_asset_path(fullname) + '.py'
        return os.path.isfile(filepath) \
            and self.is_package_dir(os.path.dirname(filepath))

sys.meta_path.append(ModuleFinder())
