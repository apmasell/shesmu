export function blank() {
  return [];
}

export function collapse(title, ...inner) {
  const items = inner.flat(Number.MAX_VALUE);
  if (items.length == 0) return [];
  const showHide = document.createElement("P");
  showHide.className = "collapse close";
  showHide.innerText = title;
  showHide.onclick = e => toggleCollapse(showHide);
  const contents = document.createElement("DIV");
  items.forEach(item => contents.appendChild(item));
  return [showHide, contents];
}

export function toggleCollapse(title) {
  const visible = !title.nextSibling.style.maxHeight;

  title.className = visible ? "collapse open" : "collapse close";
  title.nextSibling.style.maxHeight = visible
    ? `${title.nextSibling.scrollHeight}px`
    : null;
}


export function formatTimeSpan(x) {
  let diff = Math.abs(Math.ceil(x / 1000));
  let result = "";
  let chunkcount = 0;
  for (let [span, name] of [
    [31557600, "y"],
    [86400, "d"],
    [3600, "h"],
    [60, "m"],
    [1, "s"]
  ]) {
    const chunk = Math.floor(diff / span);
    if (chunk > 0 || chunkcount > 0) {
      result = `${result}${chunk}${name} `;
      diff = diff % span;
      if (++chunkcount > 2) {
        break;
      }
    }
  }
  return result;
}

export function formatTimeBin(x) {
  const d = new Date(x);
  const span = new Date() - d;
  let ago = formatTimeSpan(span);
  if (ago) {
    ago = ago + (span < 0 ? "from now" : "ago");
  } else {
    ago = "now";
  }
  return [ago, `${d.toISOString()}`];
}

export function jsonParameters(action) {
  return objectTable(action.parameters, "Parameters", x =>
    JSON.stringify(x, null, 2)
  );
}

export function link(url, t) {
  const element = document.createElement("A");
  element.innerText = t + " 🔗";
  element.target = "_blank";
  element.href = url;
  return element;
}

export function objectTable(object, title, valueFormatter) {
  return collapse(
    title,
    table(
      Object.entries(object).sort((a, b) => a[0].localeCompare(b[0])),
      ["Name", x => x[0]],
      ["Value", x => valueFormatter(x[1])]
    )
  );
}

export function preformatted(text) {
  const pre = document.createElement("PRE");
  pre.style.overflowX = "scroll";
  pre.innerText = text;
  return pre;
}


export function table(rows, ...headers) {
  if (rows.length == 0) return [];
  const table = document.createElement("TABLE");
  const headerRow = document.createElement("TR");
  table.appendChild(headerRow);
  for (const [name, func] of headers) {
    const column = document.createElement("TH");
    column.innerText = name;
    headerRow.appendChild(column);
  }
  for (const row of rows) {
    const dataRow = document.createElement("TR");
    for (const [name, func] of headers) {
      const cell = document.createElement("TD");
      const result = func(row);
      if (Array.isArray(result)) {
        result.flat(Number.MAX_VALUE).forEach(x => cell.appendChild(x));
      } else if (result instanceof HTMLElement) {
        cell.appendChild(result);
      } else {
        cell.innerText = result;
      }
      dataRow.appendChild(cell);
    }
    table.appendChild(dataRow);
  }
  return table;
}

export function text(t) {
  const element = document.createElement("P");
  element.innerText = t.replace(/\n/g, "⏎");
  return element;
}

export function timespan(title, time) {
  if (!time) return [];
  const [ago, absolute] = formatTimeBin(time);
  return text(`${title}: ${absolute} (${ago})`);
}

export function title(action, t) {
  const element = action.url ? link(action.url, t) : text(t);
  element.title = action.state;
  let tags;
  if (action.tags.length > 0) {
    tags = document.createElement("DIV");
    tags.className = "filterlist";
    tags.innerText = "Tags: ";
    action.tags.forEach(tag => {
      const button = document.createElement("SPAN");
      button.innerText = tag;
      tags.appendChild(button);
      tags.appendChild(document.createTextNode(" "));
    });
  } else {
    tags = [];
  }
  return [
    element,
    table(
      action.locations,
      ["File", l => l.file],
      ["Line", l => l.line],
      ["Column", l => l.column],
      [
        "Time",
        l => {
          const [ago, absolute] = formatTimeBin(l.time);
          return `${absolute} (${ago})`;
        }
      ],
      ["Source", l => (l.url ? link(l.url, "View Source") : blank())]
    ),
    table(
      [
        ["Last Checked", "lastChecked"],
        ["Last Added", "lastAdded"],
        ["Last Status Change", "lastStatusChange"],
        ["External Last Modification", "external"]
      ],
      ["Event", x => x[0]],
      [
        "Time",
        x => {
          const time = action[x[1]];
          if (!time) return "Unknown";
          const [ago, absolute] = formatTimeBin(time);
          return `${absolute} (${ago})`;
        }
      ]
    ),
    tags
  ];
}

export function visibleText(text) {
  return text.replace(" ", "␣").replace("\n", "⏎");
}
