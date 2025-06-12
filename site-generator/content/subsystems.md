---
title: About
description: |
  Roq is a powerful static site generator that combines the best features of tools like Jekyll and Hugo, but within the Java ecosystem. It offers a modern approach with Quarkus at its core, requiring zero configuration to get started —ideal for developers who want to jump right in, while still being flexible enough for advanced users to hook into Java for deeper customization.
layout: :theme/page
---

# About Roq

Roq is a powerful static site generator that combines the best features of tools like Jekyll and Hugo, but within the Java ecosystem. It offers a modern approach with Quarkus at its core, requiring zero configuration to get started —ideal for developers who want to jump right in, while still being flexible enough for advanced users to hook into Java for deeper customization.

## Resource

<div>
  <!-- authors.yml is in the data/ -->
  {#for subsystem in cdi:model.model}
<h3>{subsystem.address}</h3>

  {subsystem.result.description}

    {#for children in subsystem.result.children}
      {#for resource in children.value.model-description}
<h4>{children.key}={resource.key}</h4>
<p>{resource.value.description}</p>
      {/for}
    {/for}
  {/for}
</div>

