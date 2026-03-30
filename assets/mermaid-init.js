import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';

const SELECTOR = 'pre code.language-mermaid, pre code.lang-mermaid';

function decodeHtmlEntities(input) {
  const textarea = document.createElement('textarea');
  textarea.innerHTML = input;
  return textarea.value;
}

function convertMermaidCodeBlocks() {
  const codeBlocks = Array.from(document.querySelectorAll(SELECTOR));

  codeBlocks.forEach((codeBlock, index) => {
    const pre = codeBlock.closest('pre');
    if (!pre) {
      return;
    }

    const source = decodeHtmlEntities(codeBlock.textContent || '');
    const container = document.createElement('div');
    container.className = 'mermaid';
    container.id = `mermaid-diagram-${index + 1}`;
    container.textContent = source.trim();

    pre.replaceWith(container);
  });

  return codeBlocks.length;
}

function addFallbackNotice(error) {
  console.error('Mermaid initialization failed:', error);
}

async function initMermaid() {
  try {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'loose',
      theme: 'default',
    });

    const diagramsCount = convertMermaidCodeBlocks();
    if (diagramsCount > 0) {
      await mermaid.run({ querySelector: '.mermaid' });
    }
  } catch (error) {
    addFallbackNotice(error);
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initMermaid, { once: true });
} else {
  initMermaid();
}
