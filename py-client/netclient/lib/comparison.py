def bin_search(data, query):
    ln = len(query)
    if ln == 0:
        return -1
    left_anchor = 0
    right_anchor = len(data)
    flag = -1
    while True:
        if left_anchor >= right_anchor:
            return flag
        ind = (left_anchor + right_anchor)/2
        comp = data[ind]
        ln2 = len(comp)

        if query == comp:
            return ind
        if ln < ln2:
            comp = comp[:ln]
        if query == comp:
            flag = ind
        if ind == left_anchor or ind == right_anchor:
            return flag
        elif query > comp:
            left_anchor = ind
        else:
            right_anchor = ind

def levenshtein(a, b):
    """
    Calculates the Levenshtein distance between a and b.
    """
    n, m = len(a), len(b)
    if n > m:
        a, b = b, a
        n, m = m, n
    current = range(0, n+1)
    for i in range(m):
        previous, current = current, [i+1]
        for j in range(n):
            add, delete = previous[j+1]+1, current[j]+1
            change = previous[j]
            if a[j] != b[i]:
                change += 1
            current.append(min(add, delete, change))
    return current[n]
