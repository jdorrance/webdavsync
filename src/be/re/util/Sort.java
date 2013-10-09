package be.re.util;

/**
 * Some array sort methods.
 * @author Werner Donn\u00e9
 */

public class Sort

{

  private static int
  partition(Object[] array, int start, int end, int pivot, Compare criterion)
  {
    int	i = start;
    int	j = end;
    int	result = pivot;

    while (i < j)
    {
      for
      (
        ;
        i < result && criterion.compare(array[i], array[result]) <= 0;
        ++i
      );

      for
      (
        ;
        j > result && criterion.compare(array[j], array[result]) >= 0;
        --j
      );

      if (i == result)
      {
        result = j;
      }
      else
      {
        if (j == result)
        {
          result = i;
        }
      }

      Object	tmp = array[i];

      array[i] = array[j];
      array[j] = tmp;
    }

    return result;
  }



  /**
   * Sort any array according to a supplied criterion using the quick sort
   * algorithm due to Hoare. The array itself is returned.
   * @see Compare
   */

  public static Object[]
  qsort(Object[] array, Compare criterion)
  {
    qsort(array, 0, array.length - 1, criterion);

    return array;
  }



  private static void
  qsort(Object[] array, int start, int end, Compare criterion)
  {
    int	pivot = start + (end - start + 1) / 2;

    pivot = partition(array, start, end, pivot, criterion);

    if (pivot - 1 - start > 0)
    {
      qsort(array, start, pivot - 1, criterion);
    }

    if (end - pivot > 0)
    {
      qsort(array, pivot, end, criterion);
    }
  }

} // Sort
