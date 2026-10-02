import path from 'node:path';

const dependencyRoot = process.env.BBAU_NODE_MODULES || path.resolve('node_modules');

export default {
  base: './',
  plugins: [{
    name: 'github-pages-public-assets',
    enforce: 'pre',
    transform(code, id) {
      if (id.endsWith('/src/main.jsx')) {
        return code.replace(/src="\/(university-logo\.svg|central-library-exterior\.jpeg|library-main-hall\.jpeg|library-bookshelves\.jpeg|library-reading-room\.jpeg|library-study-area\.jpeg)"/g, 'src={import.meta.env.BASE_URL + "$1"}');
      }
      if (id.endsWith('/src/styles.css')) {
        return code.replaceAll("url('/central-library-exterior.jpeg')", "url('../central-library-exterior.jpeg')");
      }
      return null;
    },
  }],
  resolve: {
    alias: [
      { find: 'react', replacement: path.join(dependencyRoot, 'react') },
      { find: 'react-dom', replacement: path.join(dependencyRoot, 'react-dom') },
      { find: 'lucide-react', replacement: path.join(dependencyRoot, 'lucide-react') },
      { find: 'tesseract.js', replacement: path.join(dependencyRoot, 'tesseract.js') },
    ],
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  preview: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
};
